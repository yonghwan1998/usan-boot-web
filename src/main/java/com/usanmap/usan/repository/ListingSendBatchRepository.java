package com.usanmap.usan.repository;

import com.usanmap.usan.entity.ListingSendBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingSendBatchRepository extends JpaRepository<ListingSendBatch, Long> {
    Page<ListingSendBatch> findAllByUserIdOrderByRequestedAtDescIdDesc(Long userId, Pageable pageable);
    Optional<ListingSendBatch> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select b from ListingSendBatch b
            where b.id > :afterId and b.requestedAt >= :from and b.requestedAt <= :until
              and b.providerRequestId is not null and b.providerRequestId <> ''
              and exists (select h.id from ListingSendHistory h
                  where h.batchId = b.id and h.userId = b.userId
                    and h.deliveryStatus in ('PENDING', 'ACCEPTED', 'UNKNOWN')
                    and coalesce(h.refundedCredit, 0) = 0)
            order by b.id asc
            """)
    List<ListingSendBatch> findPendingResultBatches(@Param("from") LocalDateTime from,
                                                   @Param("until") LocalDateTime until,
                                                   @Param("afterId") Long afterId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ListingSendBatch b where b.id = :id")
    Optional<ListingSendBatch> findLockedById(@Param("id") Long id);
}
