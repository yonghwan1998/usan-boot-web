package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditOrderRepository extends JpaRepository<CreditOrder, Long> {

    Optional<CreditOrder> findByOrderNo(String orderNo);

    List<CreditOrder> findByMemberAndOrderStatusOrderByOrderedAtDesc(User member, OrderStatus orderStatus);
}
