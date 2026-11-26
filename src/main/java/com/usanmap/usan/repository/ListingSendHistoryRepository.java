package com.usanmap.usan.repository;

import java.util.Optional;
import com.usanmap.usan.entity.ListingSendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingSendHistoryRepository extends JpaRepository<ListingSendHistory, Long> {

    boolean existsByListingIdAndTrackingToken(Long listingId, String token);

    Optional<ListingSendHistory> findByTrackingTokenAndListingId(String token, Long listingId);

    List<ListingSendHistory> findAllByListingIdOrderBySentAtDesc(Long listingId);
    List<ListingSendHistory> findAllByBatchIdOrderByIdAsc(Long batchId);
    Page<ListingSendHistory> findAllByUserIdAndBatchIdIsNullOrderBySentAtDescIdDesc(Long userId, Pageable pageable);
    boolean existsByUserIdAndBatchIdIsNull(Long userId);

    @Query("""
            select h from ListingSendHistory h
            where h.batchId = :batchId and h.userId = :userId
              and (:filter = 'ALL'
                or (:filter = 'REFUNDED' and h.refundedCredit > 0)
                or (:filter = 'DELIVERED' and h.deliveryStatus = 'DELIVERED' and coalesce(h.refundedCredit, 0) = 0)
                or (:filter = 'SENDING' and h.deliveryStatus in ('PENDING', 'ACCEPTED') and coalesce(h.refundedCredit, 0) = 0))
            order by h.id asc
            """)
    Page<ListingSendHistory> findRecipients(@Param("batchId") Long batchId, @Param("userId") Long userId,
                                          @Param("filter") String filter, Pageable pageable);

    interface BatchCounts {
        Long getBatchId();
        Long getSuccessCount();
        Long getRefundCount();
        Long getUnknownCount();
    }

    @Query("""
            select h.batchId as batchId,
                sum(case when h.deliveryStatus = 'DELIVERED' then 1 else 0 end) as successCount,
                sum(case when h.refundedCredit > 0 then 1 else 0 end) as refundCount,
                sum(case when h.deliveryStatus in ('PENDING', 'ACCEPTED', 'UNKNOWN') then 1 else 0 end) as unknownCount
            from ListingSendHistory h
            where h.userId = :userId and h.batchId in :batchIds
            group by h.batchId
            """)
    List<BatchCounts> countByBatches(@Param("userId") Long userId, @Param("batchIds") List<Long> batchIds);
}
