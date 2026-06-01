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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        Pageable pageable = PageRequest.of(0, 50);
        if (type == null) {
            return creditLedgerRepository.findByMemberOrderByCreatedAtDesc(user, pageable).getContent();
        }
        return creditLedgerRepository.findByMemberAndLedgerTypeOrderByCreatedAtDesc(user, type, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long userId, String type) {
        int balance = getBalance(userId);

        LedgerType ledgerType = "ALL".equals(type) ? null : LedgerType.valueOf(type);
        List<CreditLedger> ledgers = getLedgers(userId, ledgerType);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM.dd");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        List<Map<String, Object>> items = ledgers.stream().map(l -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", l.getCreatedAt().format(dateFmt));
            item.put("title", resolveLedgerTitle(l.getLedgerType()));
            int amount = l.getChangeAmount();
            item.put("amountText", (amount > 0 ? "+" : "") + amount + "C");
            item.put("amountClass", amount > 0 ? "plus" : "minus");
            item.put("bottomText", resolveBottomText(l, timeFmt));
            return item;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", balance);
        result.put("total", items.size());
        result.put("items", items);
        return result;
    }

    private String resolveLedgerTitle(LedgerType type) {
        return switch (type) {
            case CHARGE -> "크레딧 충전";
            case USE -> "크레딧 차감";
            case CANCEL -> "크레딧 환불";
            case EXPIRE -> "크레딧 만료";
            case ADJUST -> "크레딧 조정";
        };
    }

    private String resolveBottomText(CreditLedger l, DateTimeFormatter timeFmt) {
        String time = l.getCreatedAt().format(timeFmt);
        if (l.getLedgerType() == LedgerType.CHARGE && l.getPayment() != null) {
            return time + " | " + String.format("%,d", l.getPayment().getRequestedAmount()) + "원";
        }
        return time + (l.getDescription() != null ? " | " + l.getDescription() : "");
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
    public void deductForShare(Long userId, int count, Long listingId) {
        User user = userRepository.getReferenceById(userId);
        MemberCreditBalance balance = memberCreditBalanceRepository.findByMemberWithLock(user)
                .orElseThrow(() -> new IllegalStateException("크레딧 잔액 정보가 없습니다."));

        balance.deductBalance(count);

        CreditLedger ledger = CreditLedger.builder()
                .member(user)
                .ledgerType(LedgerType.USE)
                .changeAmount(-count)
                .balanceAfter(balance.getBalance())
                .relatedType("LISTING")
                .relatedId(listingId)
                .description("매물 발송 (" + count + "명)")
                .build();
        creditLedgerRepository.save(ledger);
    }

    public record CheckoutInfo(String productName, int amount, String customerEmail, String customerName, String userPhone) {}

    public record BankTransferResult(String orderNo, String userPhone, String productName, int amount, int totalCredit) {}


    @Transactional(readOnly = true)
    public CheckoutInfo getCheckoutInfo(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        CreditProduct product = creditProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return new CheckoutInfo(
                product.getProductName(),
                product.getPriceAmount(),
                user.getEmail() != null ? user.getEmail() : "",
                user.getNickname() != null ? user.getNickname() : "",
                user.getPhone()
        );
    }

    @Transactional(readOnly = true)
    public List<CreditOrder> getPendingBankOrders() {
        return creditOrderRepository.findAllByOrderStatusWithMember(OrderStatus.PENDING_BANK);
    }

    @Transactional
    public BankTransferResult createBankTransferOrder(Long userId, Long productId, String depositorName, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        CreditProduct product = creditProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getIsActive()) {
            throw new IllegalStateException("비활성 상품입니다.");
        }

        String orderNo = "BNK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : user.getPhone();

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
                .depositorName(depositorName)
                .contactPhone(resolvedPhone)
                .orderStatus(OrderStatus.PENDING_BANK)
                .build();
        creditOrderRepository.save(order);

        Payment payment = Payment.builder()
                .creditOrder(order)
                .pgProvider(PgProvider.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.READY)
                .requestedAmount(product.getPriceAmount())
                .build();
        paymentRepository.save(payment);

        return new BankTransferResult(orderNo, resolvedPhone, product.getProductName(), product.getPriceAmount(), product.getTotalCreditAmount());
    }

    @Transactional
    public BankTransferResult approveBankTransfer(Long orderId) {
        CreditOrder order = creditOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (order.getOrderStatus() != OrderStatus.PENDING_BANK) {
            throw new IllegalStateException("무통장 입금 대기 상태가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        Payment payment = paymentRepository.findByCreditOrder(order)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));
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
                .description(order.getProductNameSnapshot() + " 충전 (무통장)")
                .build();
        creditLedgerRepository.save(ledger);

        return new BankTransferResult(order.getOrderNo(), order.getContactPhone(), order.getProductNameSnapshot(), order.getPriceAmountSnapshot(), order.getTotalCreditSnapshot());
    }

    @Transactional
    public void confirmTossCharge(String orderNo, Long userId, String paymentKey, String method, int approvedAmount, String rawJson) {
        CreditOrder order = creditOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(userId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }
        if (order.getOrderStatus() != OrderStatus.READY) {
            throw new IllegalStateException("이미 처리된 주문입니다.");
        }
        if (approvedAmount != order.getPriceAmountSnapshot()) {
            throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        Payment payment = paymentRepository.findByCreditOrder(order)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));
        payment.setPaymentKey(paymentKey);
        payment.setMethod(method);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setApprovedAmount(approvedAmount);
        payment.setApprovedAt(now);
        payment.setRawResponseJson(rawJson);

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
