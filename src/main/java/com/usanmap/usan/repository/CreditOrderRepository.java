package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CreditOrderRepository extends JpaRepository<CreditOrder, Long> {

    Optional<CreditOrder> findByOrderNo(String orderNo);

    List<CreditOrder> findByMemberAndOrderStatusOrderByOrderedAtDesc(User member, OrderStatus orderStatus);

    @Query("SELECT o FROM CreditOrder o JOIN FETCH o.member WHERE o.orderStatus = :status ORDER BY o.orderedAt DESC")
    List<CreditOrder> findAllByOrderStatusWithMember(OrderStatus status);
}
