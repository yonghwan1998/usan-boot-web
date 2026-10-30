package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CreditOrderRepository extends JpaRepository<CreditOrder, Long> {

    Optional<CreditOrder> findByOrderNo(String orderNo);

    List<CreditOrder> findByMemberAndOrderStatusOrderByOrderedAtDesc(User member, OrderStatus orderStatus);

    @Query("SELECT o FROM CreditOrder o JOIN FETCH o.member WHERE o.orderStatus = :status ORDER BY o.orderedAt DESC")
    List<CreditOrder> findAllByOrderStatusWithMember(OrderStatus status);

    /** 노티/리턴 동시 처리로 인한 중복 적립을 막기 위한 비관적 락 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CreditOrder o WHERE o.orderNo = :orderNo")
    Optional<CreditOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CreditOrder o WHERE o.id = :id")
    Optional<CreditOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT o.id FROM CreditOrder o WHERE o.orderStatus = :orderStatus " +
            "AND o.orderedAt <= :cutoff AND EXISTS (" +
            "SELECT p.id FROM Payment p WHERE p.creditOrder = o " +
            "AND p.pgProvider = :pgProvider AND p.paymentStatus = :paymentStatus" +
            ") ORDER BY o.id")
    List<Long> findStaleHectoOrderIds(
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("pgProvider") PgProvider pgProvider,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );
}
