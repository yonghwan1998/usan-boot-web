package com.usanmap.usan.repository;

import com.usanmap.usan.entity.HectoPaymentNotification;
import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HectoPaymentNotificationRepository extends JpaRepository<HectoPaymentNotification, Long> {

    boolean existsByEventKey(String eventKey);

    @Query("SELECT n.id FROM HectoPaymentNotification n " +
            "WHERE n.status = :status AND n.nextAttemptAt <= :now ORDER BY n.id")
    List<Long> findDueIds(
            @Param("status") PaymentNotificationStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM HectoPaymentNotification n WHERE n.id = :id")
    Optional<HectoPaymentNotification> findByIdForUpdate(@Param("id") Long id);
}
