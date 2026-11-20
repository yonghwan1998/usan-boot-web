package com.usanmap.usan.repository;

import com.usanmap.usan.entity.ListingSendBatch;
import com.usanmap.usan.entity.ListingSendHistory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ListingSendRepositoryTests {
    @Test
    void aggregatesKnownResultsAndScopesBatchesAndLegacyHistoryByOwner() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(ListingSendBatch.class).addAnnotatedClass(ListingSendHistory.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:listing-send-query;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            LocalDateTime now = LocalDateTime.of(2026, 11, 20, 10, 0);
            ListingSendBatch first = batch(1L, now);
            ListingSendBatch second = batch(1L, now);
            ListingSendBatch foreign = batch(2L, now);
            em.persist(first); em.persist(second); em.persist(foreign);
            em.persist(history(1L, first.getId(), "DELIVERED", 0, now));
            em.persist(history(1L, first.getId(), "FAILED", 1, now));
            em.persist(history(1L, first.getId(), "ACCEPTED", 0, now));
            em.persist(history(1L, first.getId(), "UNKNOWN", null, now));
            em.persist(history(1L, second.getId(), "DELIVERED", 0, now));
            em.persist(history(2L, foreign.getId(), "DELIVERED", 0, now));
            em.persist(history(1L, null, "UNKNOWN", null, now));
            em.persist(history(2L, null, "UNKNOWN", null, now));
            em.flush(); em.clear();

            var repositories = new JpaRepositoryFactory(em);
            var batches = repositories.getRepository(ListingSendBatchRepository.class);
            var histories = repositories.getRepository(ListingSendHistoryRepository.class);
            var page = batches.findAllByUserIdOrderByRequestedAtDescIdDesc(1L, PageRequest.of(0, 20));
            assertThat(page.getContent()).extracting(ListingSendBatch::getId)
                    .containsExactly(second.getId(), first.getId());
            assertThat(batches.findByIdAndUserId(foreign.getId(), 1L)).isEmpty();
            var counts = histories.countByBatches(1L, List.of(first.getId(), foreign.getId()));
            assertThat(counts).hasSize(1);
            assertThat(counts.get(0).getBatchId()).isEqualTo(first.getId());
            assertThat(counts.get(0).getSuccessCount()).isEqualTo(1);
            assertThat(counts.get(0).getRefundCount()).isEqualTo(1);
            assertThat(counts.get(0).getUnknownCount()).isEqualTo(2);
            assertThat(histories.findAllByUserIdAndBatchIdIsNullOrderBySentAtDescIdDesc(1L, PageRequest.of(0, 50))
                    .getContent()).hasSize(1).allMatch(h -> h.getUserId().equals(1L) && h.getBatchId() == null);
            assertThat(histories.findRecipients(first.getId(), 1L, "ALL", PageRequest.of(0, 50)).getTotalElements()).isEqualTo(4);
            assertThat(histories.findRecipients(first.getId(), 1L, "DELIVERED", PageRequest.of(0, 50)).getContent())
                    .hasSize(1).allMatch(h -> "DELIVERED".equals(h.getDeliveryStatus()));
            assertThat(histories.findRecipients(first.getId(), 1L, "SENDING", PageRequest.of(0, 50)).getContent())
                    .hasSize(1).allMatch(h -> "ACCEPTED".equals(h.getDeliveryStatus()));
            assertThat(histories.findRecipients(first.getId(), 1L, "REFUNDED", PageRequest.of(0, 50)).getContent())
                    .hasSize(1).allMatch(h -> h.getRefundedCredit() == 1);
            assertThat(histories.findRecipients(foreign.getId(), 1L, "ALL", PageRequest.of(0, 50)).getContent()).isEmpty();
            ListingSendBatch boundary = batch(1L, now.minusMinutes(10));
            ListingSendBatch expired = batch(1L, now.minusMinutes(10).minusSeconds(1));
            ListingSendBatch future = batch(1L, now.plusSeconds(1));
            ListingSendBatch failed = batch(1L, now);
            ListingSendBatch refunded = batch(1L, now);
            ListingSendBatch noReceipt = batch(1L, now);
            org.springframework.test.util.ReflectionTestUtils.setField(noReceipt, "providerRequestId", null);
            for (var b : List.of(boundary, expired, future, failed, refunded, noReceipt)) em.persist(b);
            em.persist(history(1L, boundary.getId(), "PENDING", 0, now));
            em.persist(history(1L, expired.getId(), "ACCEPTED", 0, now));
            em.persist(history(1L, future.getId(), "ACCEPTED", 0, now));
            em.persist(history(1L, failed.getId(), "FAILED", 0, now));
            em.persist(history(1L, refunded.getId(), "ACCEPTED", 1, now));
            em.persist(history(1L, noReceipt.getId(), "ACCEPTED", 0, now));
            em.flush(); em.clear();
            assertThat(batches.findPendingResultBatches(now.minusMinutes(10), now, 0L, PageRequest.of(0, 100)))
                    .extracting(ListingSendBatch::getId).containsExactly(first.getId(), boundary.getId());
            assertThat(batches.findPendingResultBatches(now.minusMinutes(10), now, first.getId(), PageRequest.of(0, 100)))
                    .extracting(ListingSendBatch::getId).containsExactly(boundary.getId());
            assertThat(batches.findLockedById(first.getId())).isPresent();
            em.getTransaction().rollback();
        }
    }

    private ListingSendBatch batch(Long userId, LocalDateTime time) {
        return ListingSendBatch.builder().listingId(9L).userId(userId).listingTitle("매물")
                .listingAddress("주소").recipientCount(3).requestStatus("ACCEPTED").messageType("LMS")
                .providerRequestId("request-" + java.util.UUID.randomUUID())
                .requestedAt(time).updatedAt(time).build();
    }

    private ListingSendHistory history(Long userId, Long batchId, String status, Integer refund, LocalDateTime time) {
        return ListingSendHistory.builder().listingId(9L).userId(userId).batchId(batchId)
                .brokerPhone("01012345678").deliveryStatus(status).refundedCredit(refund).sentAt(time).build();
    }
}
