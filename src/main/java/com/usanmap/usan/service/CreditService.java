package com.usanmap.usan.service;

import com.usanmap.usan.entity.*;
import com.usanmap.usan.entity.enums.*;
import com.usanmap.usan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditProductRepository creditProductRepository;
    private final CreditOrderRepository creditOrderRepository;
    private final PaymentRepository paymentRepository;
    private final MemberCreditBalanceRepository memberCreditBalanceRepository;
    private final CreditLedgerRepository creditLedgerRepository;
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

    @Transactional(readOnly = true)
    public List<Payment> getPayments(Long userId, PaymentStatus status) {
        Pageable pageable = PageRequest.of(0, 50);
        if (status == null) {
            return paymentRepository.findByMemberId(userId, pageable).getContent();
        }
        return paymentRepository.findByMemberIdAndStatus(userId, status, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public List<CreditLedger> getLedgers(Long userId, LedgerType type) {
        User user = userRepository.getReferenceById(userId);
        return creditLedgerRepository.findByMemberAndLedgerTypeOrderByCreatedAtDesc(user, type, PageRequest.of(0, 50))
                .getContent();
    }

    @Transactional(readOnly = true)
    public CreditOrder getCompletedOrder(String orderNo, Long userId) {
        CreditOrder order = creditOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        if (!order.getMember().getId().equals(userId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }
        if (order.getOrderStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("완료된 주문이 아닙니다.");
        }
        return order;
    }

    @Transactional
    public String createOrder(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        CreditProduct product = creditProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getIsActive()) {
            throw new IllegalStateException("비활성 상품입니다.");
        }

        String orderNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        CreditOrder order = CreditOrder.builder()
                .orderNo(orderNo)
                .member(user)
                .creditProduct(product)
                .productCodeSnapshot(product.getProductCode())
                .productNameSnapshot(product.getProductName())
                .priceAmountSnapshot(product.getPriceAmount())
                .baseCreditSnapshot(product.getBaseCreditAmount())
                .bonusCreditSnapshot(product.getBonusCreditAmount())
                .totalCreditSnapshot(product.getTotalCreditAmount())
                .orderStatus(OrderStatus.READY)
                .build();
        creditOrderRepository.save(order);

        Payment payment = Payment.builder()
                .creditOrder(order)
                .pgProvider(PgProvider.TOSS)
                .paymentStatus(PaymentStatus.READY)
                .requestedAmount(product.getPriceAmount())
                .build();
        paymentRepository.save(payment);

        return orderNo;
    }

    @Transactional
    public void mockConfirm(String orderNo, Long userId) {
        CreditOrder order = creditOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(userId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }
        if (order.getOrderStatus() != OrderStatus.READY) {
            throw new IllegalStateException("이미 처리된 주문입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        Payment payment = paymentRepository.findByCreditOrder(order)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));
        payment.setPaymentKey("MOCK-" + orderNo);
        payment.setMethod("MOCK");
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setApprovedAmount(order.getPriceAmountSnapshot());
        payment.setApprovedAt(now);

        order.setOrderStatus(OrderStatus.PAID);
        order.setPaidAt(now);

        User user = order.getMember();
        MemberCreditBalance balance = memberCreditBalanceRepository.findByMemberWithLock(user)
                .orElseGet(() -> memberCreditBalanceRepository.save(
                        MemberCreditBalance.builder().member(user).balance(0).build()
                ));
        balance.addBalance(order.getTotalCreditSnapshot());

        CreditLedger ledger = CreditLedger.builder()
                .member(user)
                .creditOrder(order)
                .payment(payment)
                .ledgerType(LedgerType.CHARGE)
                .changeAmount(order.getTotalCreditSnapshot())
                .balanceAfter(balance.getBalance())
                .description(order.getProductNameSnapshot() + " 충전")
                .build();
        creditLedgerRepository.save(ledger);
    }
}
