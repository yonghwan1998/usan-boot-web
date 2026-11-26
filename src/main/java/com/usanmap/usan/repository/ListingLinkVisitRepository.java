package com.usanmap.usan.repository;

import com.usanmap.usan.entity.ListingLinkVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ListingLinkVisitRepository extends JpaRepository<ListingLinkVisit, Long> {
    interface RecipientVisits {
        Long getHistoryId();
        long getVisitCount();
        LocalDateTime getFirstVisitedAt();
        LocalDateTime getLastVisitedAt();
    }
    interface BatchVisits {
        Long getBatchId();
        long getTrackedRecipients();
        long getVisitedRecipients();
        default double getVisitRate() {
            return getTrackedRecipients() == 0 ? 0 : getVisitedRecipients() * 100.0 / getTrackedRecipients();
        }
    }

    @Query("""
            select h.id as historyId, count(v.id) as visitCount,
                min(v.visitedAt) as firstVisitedAt, max(v.visitedAt) as lastVisitedAt
            from ListingLinkVisit v join ListingSendHistory h on h.id = v.historyId
            where h.userId = :userId and h.id in :historyIds
            group by h.id
            """)
    List<RecipientVisits> summarizeRecipients(@Param("userId") Long userId, @Param("historyIds") List<Long> historyIds);

    @Query("""
            select h.batchId as batchId, count(distinct h.id) as trackedRecipients,
                count(distinct case when v.id is not null then h.id else null end) as visitedRecipients
            from ListingSendHistory h left join ListingLinkVisit v on v.historyId = h.id
            where h.userId = :userId and h.batchId in :batchIds and h.trackingToken is not null
            group by h.batchId
            """)
    List<BatchVisits> summarizeBatches(@Param("userId") Long userId, @Param("batchIds") List<Long> batchIds);
}
