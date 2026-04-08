package com.usanmap.usan.repository;

import com.usanmap.usan.entity.CreditLedger;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.LedgerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditLedgerRepository extends JpaRepository<CreditLedger, Long> {

    Page<CreditLedger> findByMemberOrderByCreatedAtDesc(User member, Pageable pageable);

    Page<CreditLedger> findByMemberAndLedgerTypeOrderByCreatedAtDesc(User member, LedgerType ledgerType, Pageable pageable);
}
