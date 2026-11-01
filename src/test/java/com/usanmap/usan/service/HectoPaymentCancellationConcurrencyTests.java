package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.*;
import com.usanmap.usan.entity.enums.*;
import com.usanmap.usan.repository.*;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/** 실제 잔액 행 락과 커밋을 사용하며 외부 PG나 로컬·운영 DB에는 연결하지 않는다. */
@SpringJUnitConfig(HectoPaymentCancellationConcurrencyTests.TestConfig.class)
class HectoPaymentCancellationConcurrencyTests {

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private HectoPaymentCancellationTransactionService service;
    @Autowired private MemberCreditBalanceRepository balanceRepository;

    private TransactionTemplate transaction;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        reset(balanceRepository);
        transaction = new TransactionTemplate(transactionManager);
        fixture = transaction.execute(status -> {
            User user = User.builder().email(UUID.randomUUID() + "@example.com").build();
            entityManager.persist(user);
            CreditProduct product = CreditProduct.builder()
                    .productCode(UUID.randomUUID().toString()).productName("충전 상품")
                    .priceAmount(1000).baseCreditAmount(1000).bonusCreditAmount(100)
                    .totalCreditAmount(1100).build();
            entityManager.persist(product);
            CreditOrder order = CreditOrder.builder()
                    .orderNo(UUID.randomUUID().toString()).member(user).creditProduct(product)
                    .productCodeSnapshot(product.getProductCode()).productNameSnapshot("충전 상품")
                    .priceAmountSnapshot(1000).baseCreditSnapshot(1000).bonusCreditSnapshot(100)
                    .totalCreditSnapshot(1100).orderStatus(OrderStatus.PAID)
                    .paidAt(LocalDateTime.now().minusDays(1)).build();
            entityManager.persist(order);
            Payment payment = Payment.builder().creditOrder(order).pgProvider(PgProvider.HECTO)
                    .paymentStatus(PaymentStatus.SUCCESS).method("CA").paymentKey(UUID.randomUUID().toString())
                    .requestedAmount(1000).approvedAmount(1000).build();
            entityManager.persist(payment);
            MemberCreditBalance balance = MemberCreditBalance.builder().member(user).balance(3000).build();
            entityManager.persist(balance);
            entityManager.persist(CreditLedger.builder().member(user).creditOrder(order).payment(payment)
                    .ledgerType(LedgerType.CHARGE).changeAmount(1100).balanceAfter(3000)
                    .createdAt(LocalDateTime.now().minusHours(1)).build());
            entityManager.flush();
            return new Fixture(user.getId(), balance.getId(), payment.getId());
        });
    }

    @Test
    void reservesCreditsAndPersistsRequestInOneTransaction() {
        var prepared = service.prepare(fixture.paymentId(), "고객 요청", LocalDateTime.now());

        transaction.executeWithoutResult(status -> {
            assertThat(entityManager.find(MemberCreditBalance.class, fixture.balanceId()).getBalance()).isEqualTo(1900);
            PaymentCancellation request = entityManager.find(PaymentCancellation.class, prepared.cancellationId());
            assertThat(request.getStatus()).isEqualTo(PaymentCancellationStatus.REQUESTED);
            assertThat(request.getReservedCredit()).isEqualTo(1100);
        });
    }

    @Test
    void rejectsUsageCommittedWhileWaitingForBalanceLock() throws Exception {
        CountDownLatch usageHasLock = new CountDownLatch(1);
        CountDownLatch cancellationAttemptedLock = new CountDownLatch(1);
        CountDownLatch allowUsageCommit = new CountDownLatch(1);
        MemberCreditBalanceRepository actualRepository =
                new JpaRepositoryFactory(entityManager).getRepository(MemberCreditBalanceRepository.class);
        doAnswer(invocation -> {
            int isolation = entityManager.unwrap(Session.class).doReturningWork(Connection::getTransactionIsolation);
            assertThat(isolation).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
            cancellationAttemptedLock.countDown();
            return actualRepository.findByMemberWithLock(invocation.getArgument(0));
        }).when(balanceRepository).findByMemberWithLock(any(User.class));

        var executor = Executors.newFixedThreadPool(2);
        try {
            var usage = executor.submit(() -> transaction.executeWithoutResult(status -> {
                MemberCreditBalance balance = entityManager.find(
                        MemberCreditBalance.class, fixture.balanceId(), LockModeType.PESSIMISTIC_WRITE);
                balance.deductBalance(100);
                entityManager.persist(CreditLedger.builder()
                        .member(entityManager.getReference(User.class, fixture.userId()))
                        .ledgerType(LedgerType.USE).changeAmount(-100).balanceAfter(2900).build());
                entityManager.flush();
                usageHasLock.countDown();
                await(allowUsageCommit);
            }));
            assertThat(usageHasLock.await(10, TimeUnit.SECONDS)).isTrue();
            var cancellation = executor.submit(() ->
                    assertThatThrownBy(() -> service.prepare(fixture.paymentId(), "고객 요청", LocalDateTime.now()))
                            .isInstanceOf(IllegalStateException.class).hasMessageContaining("사용 이력"));
            assertThat(cancellationAttemptedLock.await(10, TimeUnit.SECONDS)).isTrue();
            allowUsageCommit.countDown();
            usage.get(10, TimeUnit.SECONDS);
            cancellation.get(10, TimeUnit.SECONDS);

            transaction.executeWithoutResult(status -> {
                assertThat(entityManager.find(MemberCreditBalance.class, fixture.balanceId()).getBalance()).isEqualTo(2900);
                Long requests = entityManager.createQuery(
                                "SELECT COUNT(c) FROM PaymentCancellation c WHERE c.payment.id = :paymentId", Long.class)
                        .setParameter("paymentId", fixture.paymentId()).getSingleResult();
                assertThat(requests).isZero();
            });
        } finally {
            allowUsageCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("테스트 동기화 시간 초과");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private record Fixture(Long userId, Long balanceId, Long paymentId) {}

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean(destroyMethod = "close")
        HikariDataSource dataSource() {
            var dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:h2:mem:cancellation-locks;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            dataSource.setMaximumPoolSize(4);
            // MySQL의 기본 격리 수준에서도 prepare가 READ_COMMITTED로 전환하는지 확인한다.
            dataSource.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setManagedTypes(PersistenceManagedTypes.of(User.class.getName(), CreditProduct.class.getName(),
                    CreditOrder.class.getName(), Payment.class.getName(), MemberCreditBalance.class.getName(),
                    CreditLedger.class.getName(), PaymentCancellation.class.getName()));
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        EntityManager entityManager(EntityManagerFactory factory) {
            return SharedEntityManagerCreator.createSharedEntityManager(factory);
        }

        @Bean
        PaymentRepository paymentRepository(EntityManager entityManager) {
            return new JpaRepositoryFactory(entityManager).getRepository(PaymentRepository.class);
        }

        @Bean
        PaymentCancellationRepository cancellationRepository(EntityManager entityManager) {
            return new JpaRepositoryFactory(entityManager).getRepository(PaymentCancellationRepository.class);
        }

        @Bean
        CreditOrderRepository orderRepository(EntityManager entityManager) {
            return new JpaRepositoryFactory(entityManager).getRepository(CreditOrderRepository.class);
        }

        @Bean
        MemberCreditBalanceRepository balanceRepository(EntityManager entityManager) {
            var actual = new JpaRepositoryFactory(entityManager).getRepository(MemberCreditBalanceRepository.class);
            return mock(MemberCreditBalanceRepository.class, delegatesTo(actual));
        }

        @Bean
        CreditLedgerRepository ledgerRepository(EntityManager entityManager) {
            return new JpaRepositoryFactory(entityManager).getRepository(CreditLedgerRepository.class);
        }

        @Bean
        HectoPaymentCancellationTransactionService cancellationService(PaymentRepository payments,
                PaymentCancellationRepository cancellations, CreditOrderRepository orders,
                MemberCreditBalanceRepository balances, CreditLedgerRepository ledgers) {
            var profiles = new HectoPaymentProfiles("test-mid", "test-hash", "12345678901234567890123456789012", true,
                    "prod-mid", "prod-hash", "abcdefghijklmnopqrstuvwxyz123456", false);
            return new HectoPaymentCancellationTransactionService(payments, cancellations, orders, balances, ledgers, profiles);
        }
    }
}
