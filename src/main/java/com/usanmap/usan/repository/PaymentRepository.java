package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByCreditOrder(CreditOrder creditOrder);

    @Query("SELECT p FROM Payment p JOIN p.creditOrder o WHERE o.member.id = :memberId ORDER BY p.requestedAt DESC")
    Page<Payment> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN p.creditOrder o WHERE o.member.id = :memberId AND p.paymentStatus = :status ORDER BY p.requestedAt DESC")
    Page<Payment> findByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") PaymentStatus status, Pageable pageable);
}
