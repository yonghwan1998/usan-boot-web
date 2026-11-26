package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.dto.SmsDeliveryResult;
import com.usanmap.usan.entity.*;
import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.repository.*;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ListingSendRefundTransactionTests.Config.class)
class ListingSendRefundTransactionTests {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ListingSendService service;
    TransactionTemplate tx;
    Long batchId;
    Long historyId;
    Long balanceId;
    Long ledgerId;

    @BeforeEach
    void prepare() {
        tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            var user = User.builder().email(UUID.randomUUID() + "@example.com").build();
            em.persist(user);
            var balance = MemberCreditBalance.builder().member(user).balance(9).build();
            em.persist(balance);
            var ledger = CreditLedger.builder().member(user).ledgerType(LedgerType.USE)
                    .changeAmount(-1).balanceAfter(9).relatedType("LISTING").relatedId(7L).build();
            em.persist(ledger);
            var now = LocalDateTime.now();
            var batch = ListingSendBatch.builder().listingId(7L).userId(user.getId()).listingTitle("매물")
                    .listingAddress("주소").recipientCount(1).requestStatus("ACCEPTED").messageType("LMS")
                    .creditUsed(1).creditLedgerId(ledger.getId()).providerRequestId("request-1")
                    .requestedAt(now).updatedAt(now).build();
            em.persist(batch);
            var history = ListingSendHistory.builder().listingId(7L).userId(user.getId()).batchId(batch.getId())
                    .brokerPhone("01012345678").deliveryStatus("ACCEPTED").refundedCredit(0).build();
            em.persist(history);
            batchId = batch.getId(); historyId = history.getId(); balanceId = balance.getId(); ledgerId = ledger.getId();
        });
    }

    private List<SmsDeliveryResult> rejected() {
        return List.of(new SmsDeliveryResult("request-1", "message-1", "01012345678",
                "COMPLETED", "fail", "3018", LocalDateTime.now()));
    }

    @Test
    void concurrentResultsRestoreOneCreditAndOneLedgerOnly() throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> apply = () -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return service.applyDeliveryResults(batchId, "request-1", rejected());
            };
            var first = pool.submit(apply);
            var second = pool.submit(apply);
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        tx.executeWithoutResult(status -> {
            assertThat(em.find(MemberCreditBalance.class, balanceId).getBalance()).isEqualTo(10);
            var history = em.find(ListingSendHistory.class, historyId);
            assertThat(history.getRefundedCredit()).isEqualTo(1);
            var refund = em.find(CreditLedger.class, history.getRefundCreditLedgerId());
            assertThat(refund.getLedgerType()).isEqualTo(LedgerType.CANCEL);
            assertThat(refund.getChangeAmount()).isEqualTo(1);
            assertThat(refund.getBalanceAfter()).isEqualTo(10);
            assertThat(refund.getRelatedId()).isEqualTo(historyId);
            assertThat(refund.getDescription()).isEqualTo("매물 발송 실패 환불");
            assertThat(history.getResultCode()).isEqualTo("3018");
            assertThat(refundCount()).isEqualTo(1);
        });
    }

    @Test
    void rollsBackDeliveryAndBalanceWhenRefundCannotBeSaved() {
        // 환불 원장 INSERT 실패를 DB 제약으로 유발한다.
        tx.executeWithoutResult(status -> em.createNativeQuery(
                "alter table credit_ledger add constraint reject_refund check (changeAmount < 0 or relatedId <> " + historyId + ")").executeUpdate());
        try {
            assertThatThrownBy(() -> service.applyDeliveryResults(batchId, "request-1", rejected()))
                    .isInstanceOf(RuntimeException.class);
            tx.executeWithoutResult(status -> {
                assertThat(em.find(MemberCreditBalance.class, balanceId).getBalance()).isEqualTo(9);
                var history = em.find(ListingSendHistory.class, historyId);
                assertThat(history.getDeliveryStatus()).isEqualTo("ACCEPTED");
                assertThat(history.getRefundedCredit()).isZero();
                assertThat(history.getRefundCreditLedgerId()).isNull();
                assertThat(refundCount()).isZero();
            });
        } finally {
            tx.executeWithoutResult(status -> em.createNativeQuery(
                    "alter table credit_ledger drop constraint reject_refund").executeUpdate());
        }
        assertThat(service.applyDeliveryResults(batchId, "request-1", rejected())).isEqualTo(1);
    }

    @Test
    void refusesRefundWithoutMatchingOriginalDeduction() {
        tx.executeWithoutResult(status -> em.find(CreditLedger.class, ledgerId).setRelatedId(99L));
        assertThatThrownBy(() -> service.applyDeliveryResults(batchId, "request-1", rejected()))
                .isInstanceOf(IllegalStateException.class).hasMessage("발송 차감 원장이 일치하지 않습니다.");
        tx.executeWithoutResult(status -> {
            assertThat(em.find(MemberCreditBalance.class, balanceId).getBalance()).isEqualTo(9);
            assertThat(em.find(ListingSendHistory.class, historyId).getDeliveryStatus()).isEqualTo("ACCEPTED");
        });
    }

    private long refundCount() {
        return em.createQuery("select count(l) from CreditLedger l where l.relatedType = 'LISTING_SEND_HISTORY' and l.relatedId = :id", Long.class)
                .setParameter("id", historyId).getSingleResult();
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean(destroyMethod = "close")
        HikariDataSource dataSource() {
            var ds = new HikariDataSource();
            ds.setJdbcUrl("jdbc:h2:mem:listing-refund;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
            ds.setUsername("sa");
            return ds;
        }
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setManagedTypes(PersistenceManagedTypes.of(User.class.getName(), CreditProduct.class.getName(),
                    CreditOrder.class.getName(), Payment.class.getName(), MemberCreditBalance.class.getName(),
                    CreditLedger.class.getName(), ListingSendBatch.class.getName(), ListingSendHistory.class.getName()));
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean EntityManager entityManager(EntityManagerFactory factory) { return SharedEntityManagerCreator.createSharedEntityManager(factory); }
        @Bean CreditService creditService(EntityManager em) {
            var repos = new JpaRepositoryFactory(em);
            return new CreditService(mock(CreditProductRepository.class), mock(CreditOrderRepository.class),
                    mock(PaymentRepository.class), repos.getRepository(MemberCreditBalanceRepository.class),
                    repos.getRepository(CreditLedgerRepository.class), repos.getRepository(UserRepository.class), new ObjectMapper());
        }
        @Bean ListingSendService listingSendService(EntityManager em, CreditService credits) {
            var repos = new JpaRepositoryFactory(em);
            return new ListingSendService(repos.getRepository(ListingSendBatchRepository.class),
                    repos.getRepository(ListingSendHistoryRepository.class), credits, mock(ListingService.class),
                    mock(BrokerPropertyCountRepository.class), mock(ListingRepository.class));
        }
    }
}
