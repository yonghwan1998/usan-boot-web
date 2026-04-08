package com.usanmap.usan.repository;

import com.usanmap.usan.entity.MemberCreditBalance;
import com.usanmap.usan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MemberCreditBalanceRepository extends JpaRepository<MemberCreditBalance, Long> {

    Optional<MemberCreditBalance> findByMember(User member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM MemberCreditBalance b WHERE b.member = :member")
    Optional<MemberCreditBalance> findByMemberWithLock(@Param("member") User member);
}
