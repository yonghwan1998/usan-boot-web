package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditLedger;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.LedgerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CreditLedgerRepository extends JpaRepository<CreditLedger, Long> {

    Page<CreditLedger> findTop5ByMemberOrderByCreatedAtDesc(User member, Pageable pageable);

    Page<CreditLedger> findTop5ByMemberAndLedgerTypeOrderByCreatedAtDesc(User member, LedgerType ledgerType, Pageable pageable);

    Optional<CreditLedger> findFirstByPaymentAndLedgerTypeOrderByCreatedAtAsc(Payment payment, LedgerType ledgerType);

    boolean existsByMemberAndLedgerTypeAndCreatedAtAfter(User member, LedgerType ledgerType, LocalDateTime createdAt);
}
