package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
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
}
