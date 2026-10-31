package com.usanmap.usan.repository;

import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.PaymentCancellation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentCancellationRepository extends JpaRepository<PaymentCancellation, Long> {

    Optional<PaymentCancellation> findByPayment(Payment payment);

    Optional<PaymentCancellation> findByMerchantCancellationNo(String merchantCancellationNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PaymentCancellation c JOIN FETCH c.payment p JOIN FETCH p.creditOrder o WHERE c.id = :id")
    Optional<PaymentCancellation> findByIdForUpdate(@Param("id") Long id);
}
