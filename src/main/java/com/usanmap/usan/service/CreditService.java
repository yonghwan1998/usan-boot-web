package com.usanmap.usan.service;

import com.usanmap.usan.entity.CreditProduct;
import com.usanmap.usan.entity.MemberCreditBalance;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.repository.CreditProductRepository;
import com.usanmap.usan.repository.MemberCreditBalanceRepository;
import com.usanmap.usan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditProductRepository creditProductRepository;
    private final MemberCreditBalanceRepository memberCreditBalanceRepository;
    private final UserRepository userRepository;

    public List<CreditProduct> getActiveProducts() {
        return creditProductRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public int getBalance(Long userId) {
        User user = userRepository.getReferenceById(userId);
        return memberCreditBalanceRepository.findByMember(user)
                .map(MemberCreditBalance::getBalance)
                .orElse(0);
    }
}
