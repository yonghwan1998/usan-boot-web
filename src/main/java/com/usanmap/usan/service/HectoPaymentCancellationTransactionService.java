package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.CreditLedger;
import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.MemberCreditBalance;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.PaymentCancellation;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.enums.LedgerType;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditLedgerRepository;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.MemberCreditBalanceRepository;
import com.usanmap.usan.repository.PaymentCancellationRepository;
import com.usanmap.usan.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HectoPaymentCancellationTransactionService {

    private static final DateTimeFormatter CANCELLATION_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;
    private final PaymentCancellationRepository cancellationRepository;
    private final CreditOrderRepository creditOrderRepository;
    private final MemberCreditBalanceRepository balanceRepository;
    private final CreditLedgerRepository ledgerRepository;
    private final HectoPaymentProfiles paymentProfiles;

    @Transactional
    public PreparedCancellation prepare(Long paymentId, String reason, LocalDateTime requestedAt) {
        Payment initialPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));
        CreditOrder order = creditOrderRepository.findByIdForUpdate(initialPayment.getCreditOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("결제 주문을 찾을 수 없습니다."));
        Payment payment = paymentRepository.findByCreditOrder(order)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));
        if (!payment.getId().equals(paymentId)) {
            throw new IllegalStateException("결제와 주문 정보가 일치하지 않습니다.");
        }

        PaymentCancellation existing = cancellationRepository.findByPayment(payment).orElse(null);
        if (existing != null && existing.getStatus() == PaymentCancellationStatus.SUCCEEDED) {
            return PreparedCancellation.alreadySucceeded(existing);
        }
        if (existing != null && existing.getStatus() != PaymentCancellationStatus.FAILED) {
            throw new IllegalStateException("이미 처리 중이거나 확인이 필요한 결제 취소 요청입니다.");
        }

        validateCancellation(payment, order, requestedAt);
        String normalizedReason = normalizeReason(reason);
        HectoPaymentProfiles.PaymentProfile profile = paymentProfiles.forAccount(order.getMember().getEmail());
        if (!profile.isConfigured()) {
            throw new IllegalStateException("헥토파이낸셜 결제 취소 설정을 확인해주세요.");
        }

        CreditLedger chargeLedger = ledgerRepository
                .findFirstByPaymentAndLedgerTypeOrderByCreatedAtAsc(payment, LedgerType.CHARGE)
                .orElseThrow(() -> new IllegalStateException("결제 충전 내역을 찾을 수 없습니다."));
        if (ledgerRepository.existsByMemberAndLedgerTypeAndCreatedAtAfter(
                order.getMember(), LedgerType.USE, chargeLedger.getCreatedAt())) {
            throw new IllegalStateException("충전 후 크레딧 사용 이력이 있어 자동 전액 취소할 수 없습니다.");
        }

        MemberCreditBalance balance = balanceRepository.findByMemberWithLock(order.getMember())
                .orElseThrow(() -> new IllegalStateException("크레딧 잔액 정보가 없습니다."));
        int creditToReserve = order.getTotalCreditSnapshot();
        balance.deductBalance(creditToReserve);

        String cancellationNo = createCancellationNo(requestedAt);
        PaymentCancellation cancellation;
        if (existing == null) {
            cancellation = PaymentCancellation.builder()
                    .payment(payment)
                    .environment(profile.environment())
                    .merchantCancellationNo(cancellationNo)
                    .originalTransactionNo(payment.getPaymentKey())
                    .requestedAmount(payment.getApprovedAmount())
                    .reservedCredit(creditToReserve)
                    .reason(normalizedReason)
                    .status(PaymentCancellationStatus.REQUESTED)
                    .attemptCount(1)
                    .requestedAt(requestedAt)
                    .build();
            cancellationRepository.save(cancellation);
        } else {
            existing.retry(profile.environment(), cancellationNo, normalizedReason, requestedAt);
            cancellation = existing;
        }

        return PreparedCancellation.ready(cancellation, profile);
    }

    @Transactional
    public void markProcessing(Long cancellationId) {
        PaymentCancellation cancellation = findForUpdate(cancellationId);
        cancellation.markProcessing();
    }

    @Transactional
    public void complete(Long cancellationId, HectoCancellationClient.CancellationResponse response) {
        PaymentCancellation cancellation = findForUpdate(cancellationId);
        completeCancellation(cancellation, response);
    }

    @Transactional
    public ReconciliationOutcome completeFromReconciliation(
            Long cancellationId,
            String providerCancellationNo,
            String originalTransactionNo,
            int amount
    ) {
        PaymentCancellation cancellation = findForUpdate(cancellationId);
        if (cancellation.getStatus() == PaymentCancellationStatus.SUCCEEDED) {
            return ReconciliationOutcome.ALREADY_COMPLETED;
        }
        if (!cancellation.getOriginalTransactionNo().equals(originalTransactionNo)
                || cancellation.getRequestedAmount() != amount) {
            throw new IllegalStateException("거래대사 취소 정보가 취소 요청과 일치하지 않습니다.");
        }
        completeCancellation(cancellation, new HectoCancellationClient.CancellationResponse(
                true, "0021", "0000", "거래대사에서 취소 확인", providerCancellationNo, "{}"
        ));
        return ReconciliationOutcome.COMPLETED;
    }

    private void completeCancellation(PaymentCancellation cancellation,
                                      HectoCancellationClient.CancellationResponse response) {
        Payment payment = cancellation.getPayment();
        CreditOrder order = creditOrderRepository.findByIdForUpdate(payment.getCreditOrder().getId())
                .orElseThrow(() -> new IllegalStateException("결제 주문을 찾을 수 없습니다."));
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS || order.getOrderStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("현재 상태에서는 결제 취소를 확정할 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setPaymentStatus(PaymentStatus.CANCELED);
        payment.setCanceledAt(now);
        order.setOrderStatus(OrderStatus.CANCELED);
        order.setCanceledAt(now);

        MemberCreditBalance balance = balanceRepository.findByMember(order.getMember())
                .orElseThrow(() -> new IllegalStateException("크레딧 잔액 정보가 없습니다."));
        ledgerRepository.save(CreditLedger.builder()
                .member(order.getMember())
                .creditOrder(order)
                .payment(payment)
                .ledgerType(LedgerType.CANCEL)
                .changeAmount(-cancellation.getReservedCredit())
                .balanceAfter(balance.getBalance())
                .description(order.getProductNameSnapshot() + " 전액 환불 (헥토파이낸셜)")
                .build());
        cancellation.succeed(response.cancellationTransactionNo(), response.resultCode(),
                response.resultMessage(), response.auditJson(), now);
    }

    @Transactional
    public void failAndRelease(Long cancellationId, HectoCancellationClient.CancellationResponse response) {
        PaymentCancellation cancellation = findForUpdate(cancellationId);
        restoreReservedCredit(cancellation);
        cancellation.fail(response.resultCode(), response.resultMessage(), response.auditJson(), LocalDateTime.now());
    }

    @Transactional
    public void requireReview(Long cancellationId, String message) {
        PaymentCancellation cancellation = findForUpdate(cancellationId);
        cancellation.requireReview(safeMessage(message), LocalDateTime.now());
    }

    private PaymentCancellation findForUpdate(Long cancellationId) {
        return cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new IllegalArgumentException("결제 취소 요청을 찾을 수 없습니다."));
    }

    private void restoreReservedCredit(PaymentCancellation cancellation) {
        User user = cancellation.getPayment().getCreditOrder().getMember();
        MemberCreditBalance balance = balanceRepository.findByMemberWithLock(user)
                .orElseThrow(() -> new IllegalStateException("크레딧 잔액 정보가 없습니다."));
        balance.addBalance(cancellation.getReservedCredit());
    }

    private void validateCancellation(Payment payment, CreditOrder order, LocalDateTime requestedAt) {
        if (payment.getPgProvider() != PgProvider.HECTO) {
            throw new IllegalStateException("헥토파이낸셜 결제만 이 API로 취소할 수 있습니다.");
        }
        if (!"CA".equals(payment.getMethod())) {
            throw new IllegalStateException("신용카드(CA) 결제만 자동 취소할 수 있습니다.");
        }
        if (order.getOrderStatus() != OrderStatus.PAID || payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("결제 완료 상태에서만 취소할 수 있습니다.");
        }
        if (payment.getPaymentKey() == null || payment.getPaymentKey().isBlank()) {
            throw new IllegalStateException("헥토파이낸셜 원거래번호가 없습니다.");
        }
        if (payment.getApprovedAmount() == null
                || !payment.getApprovedAmount().equals(order.getPriceAmountSnapshot())) {
            throw new IllegalStateException("승인 금액과 주문 금액이 일치하지 않습니다.");
        }
        if (order.getPaidAt() == null || order.getPaidAt().isBefore(requestedAt.minusYears(1))) {
            throw new IllegalStateException("결제일로부터 1년이 지난 결제는 자동 취소할 수 없습니다.");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("취소 사유를 입력해주세요.");
        }
        String normalized = reason.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("취소 사유는 255자 이하여야 합니다.");
        }
        return normalized;
    }

    private String createCancellationNo(LocalDateTime requestedAt) {
        return "CANCEL" + requestedAt.format(CANCELLATION_NO_TIME)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String safeMessage(String message) {
        String value = message == null || message.isBlank() ? "PG 취소 결과를 확인해야 합니다." : message;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    public record PreparedCancellation(
            Long cancellationId,
            HectoPaymentProfiles.PaymentProfile profile,
            String cancellationNo,
            String originalTransactionNo,
            int amount,
            String reason,
            LocalDateTime requestedAt,
            boolean alreadySucceeded
    ) {
        static PreparedCancellation ready(PaymentCancellation cancellation,
                                          HectoPaymentProfiles.PaymentProfile profile) {
            return new PreparedCancellation(
                    cancellation.getId(), profile, cancellation.getMerchantCancellationNo(),
                    cancellation.getOriginalTransactionNo(), cancellation.getRequestedAmount(),
                    cancellation.getReason(), cancellation.getRequestedAt(), false
            );
        }

        static PreparedCancellation alreadySucceeded(PaymentCancellation cancellation) {
            return new PreparedCancellation(
                    cancellation.getId(), null, cancellation.getMerchantCancellationNo(),
                    cancellation.getOriginalTransactionNo(), cancellation.getRequestedAmount(),
                    cancellation.getReason(), cancellation.getRequestedAt(), true
            );
        }
    }

    public enum ReconciliationOutcome {
        COMPLETED,
        ALREADY_COMPLETED
    }
}
