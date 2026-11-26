package com.usanmap.usan.service;

import com.usanmap.usan.entity.*;
import com.usanmap.usan.repository.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ListingLinkVisitTests {
    @Test
    void recordsRepeatedVisitsScopesTokensToListingAndSummariesToSender() {
        Configuration config = new Configuration().addAnnotatedClass(ListingSendHistory.class)
                .addAnnotatedClass(ListingLinkVisit.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:link-visits;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        try (var factory = config.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            var first = history(1L, 10L, 20L, "Ab3x_K");
            var second = history(1L, 10L, 20L, "Ab3x_L");
            var legacy = history(1L, 10L, 20L, null);
            var foreign = history(2L, 11L, 21L, "Ab3x_K"); // 다른 매물에서는 같은 토큰을 사용할 수 있다.
            for (var h : List.of(first, second, legacy, foreign)) em.persist(h);
            em.flush();
            var repos = new JpaRepositoryFactory(em);
            var histories = repos.getRepository(ListingSendHistoryRepository.class);
            var visits = repos.getRepository(ListingLinkVisitRepository.class);
            var service = new ListingLinkVisitService(histories, visits);
            service.record(10L, "Ab3x_K");
            service.record(10L, "Ab3x_K");
            service.record(11L, "Ab3x_K");
            service.record(10L, null);
            service.record(10L, "garbage-too-long");
            service.record(99L, "Ab3x_K");
            service.record(10L, "Ab3x_Z");
            service.record(10L, "ab3x_k");
            em.flush(); em.clear();
            assertThat(visits.count()).isEqualTo(3);
            var summary = service.batches(1L, List.of(20L, 21L));
            assertThat(summary).containsOnlyKeys(20L);
            assertThat(summary.get(20L).getTrackedRecipients()).isEqualTo(2);
            assertThat(summary.get(20L).getVisitedRecipients()).isEqualTo(1);
            assertThat(summary.get(20L).getVisitRate()).isEqualTo(50.0);
            var detail = service.recipients(1L, List.of(first.getId(), second.getId(), foreign.getId()));
            assertThat(detail).containsOnlyKeys(first.getId());
            assertThat(detail.get(first.getId()).getVisitCount()).isEqualTo(2);
            assertThat(detail.get(first.getId()).getFirstVisitedAt()).isNotNull();
            assertThat(detail.get(first.getId()).getLastVisitedAt()).isAfterOrEqualTo(detail.get(first.getId()).getFirstVisitedAt());
            assertThat(service.recipients(2L, List.of(first.getId()))).isEmpty();
            assertThat(service.batches(1L, List.of())).isEmpty();
            em.getTransaction().commit();
            em.getTransaction().begin();
            assertThatThrownBy(() -> {
                em.persist(history(1L, 10L, 22L, "Ab3x_K"));
                em.flush();
            }).isInstanceOf(jakarta.persistence.PersistenceException.class);
            em.getTransaction().rollback();
        }
    }

    private ListingSendHistory history(Long user, Long listing, Long batch, String token) {
        return ListingSendHistory.builder().listingId(listing).userId(user).batchId(batch)
                .brokerPhone("01012345678").sentAt(LocalDateTime.now()).deliveryStatus("ACCEPTED")
                .trackingToken(token).build();
    }
}
