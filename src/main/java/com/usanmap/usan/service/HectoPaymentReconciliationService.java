package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.PaymentReconciliationRun;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.PaymentReconciliationRunRepository;
import com.usanmap.usan.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HectoPaymentReconciliationService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<String> RECOVERABLE_METHODS = Set.of("CA", "PZ");

    private final HectoReconciliationClient reconciliationClient;
    private final HectoPaymentProfiles paymentProfiles;
    private final PaymentReconciliationRunRepository runRepository;
    private final CreditOrderRepository creditOrderRepository;
    private final PaymentRepository paymentRepository;
    private final CreditService creditService;
    private final PaymentObservability paymentObservability;

    public ReconciliationSummary reconcile(
            LocalDate targetDate,
            HectoPaymentProfiles.Environment environment,
            boolean recover
    ) {
        if (targetDate.isAfter(LocalDate.now(SERVICE_ZONE))) {
            throw new IllegalArgumentException("미래 날짜는 거래대사할 수 없습니다.");
        }

        HectoPaymentProfiles.PaymentProfile profile = paymentProfiles.forEnvironment(environment);
        PaymentReconciliationRun run = runRepository.save(PaymentReconciliationRun.builder()
                .targetDate(targetDate)
                .environment(environment)
                .build());

        try {
            HectoReconciliationResponse response = reconciliationClient.fetch(profile, targetDate);
            Counters counters = reconcileTransactions(targetDate, profile, response.transactions(), recover);
            run.complete(
                    response.transactions().size(),
                    counters.matched,
                    counters.recovered,
                    counters.mismatched,
                    counters.cancellations
            );
            runRepository.save(run);
            paymentObservability.recordPaymentReconciliationRun(environmentName(environment), "completed");
            log.info("payment_reconciliation_completed runId={} targetDate={} environment={} providerCount={} " +
                            "matched={} recovered={} mismatched={} cancellations={} recoveryEnabled={}",
                    run.getId(), targetDate, environment, response.transactions().size(), counters.matched,
                    counters.recovered, counters.mismatched, counters.cancellations, recover);
            return new ReconciliationSummary(
                    run.getId(), targetDate, environment, response.transactions().size(), counters.matched,
                    counters.recovered, counters.mismatched, counters.cancellations, recover
            );
        } catch (RuntimeException e) {
            run.fail(e);
            runRepository.save(run);
            paymentObservability.recordPaymentReconciliationRun(environmentName(environment), "failed");
            log.error("payment_reconciliation_failed runId={} targetDate={} environment={} exceptionType={}",
                    run.getId(), targetDate, environment, e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    private Counters reconcileTransactions(
            LocalDate targetDate,
            HectoPaymentProfiles.PaymentProfile profile,
            List<HectoReconciliationTransaction> transactions,
            boolean recover
    ) {
        Counters counters = new Counters();
        Set<String> providerApprovalKeys = new HashSet<>();
        Set<String> canceledApprovalKeys = transactions.stream()
                .filter(transaction -> transaction.type() == HectoReconciliationTransaction.Type.CANCELLATION)
                .map(HectoReconciliationTransaction::originalTransactionNo)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> canceledOrderNos = transactions.stream()
                .filter(transaction -> transaction.type() == HectoReconciliationTransaction.Type.CANCELLATION)
                .map(HectoReconciliationTransaction::orderNo)
                .collect(java.util.stream.Collectors.toSet());

        for (HectoReconciliationTransaction transaction : transactions) {
            if (!profile.mchtId().equals(transaction.merchantId())) {
                counters.mismatched++;
                log.warn("payment_reconciliation_mismatch reason=merchant_id orderNo={} transactionNo={}",
                        transaction.orderNo(), transaction.transactionNo());
                continue;
            }
            if (!RECOVERABLE_METHODS.contains(transaction.method())) {
                counters.mismatched++;
                log.warn("payment_reconciliation_mismatch reason=unsupported_method orderNo={} transactionNo={} method={}",
                        transaction.orderNo(), transaction.transactionNo(), transaction.method());
                continue;
            }
            if (transaction.type() == HectoReconciliationTransaction.Type.CANCELLATION) {
                counters.cancellations++;
                reconcileCancellation(transaction, counters);
                continue;
            }

            providerApprovalKeys.add(transaction.transactionNo());
            if (canceledApprovalKeys.contains(transaction.transactionNo())
                    || canceledOrderNos.contains(transaction.orderNo())) {
                log.warn("payment_reconciliation_approval_already_canceled orderNo={} transactionNo={}",
                        transaction.orderNo(), transaction.transactionNo());
                continue;
            }
            reconcileApproval(profile, transaction, recover, counters);
        }

        reconcileMissingProviderApprovals(targetDate, profile, providerApprovalKeys, counters);
        return counters;
    }

    private void reconcileApproval(
            HectoPaymentProfiles.PaymentProfile profile,
            HectoReconciliationTransaction transaction,
            boolean recover,
            Counters counters
    ) {
        Optional<CreditOrder> optionalOrder = creditOrderRepository.findByOrderNo(transaction.orderNo());
        Optional<String> accountEmail = creditOrderRepository.findMemberEmailByOrderNo(transaction.orderNo());
        if (optionalOrder.isEmpty() || accountEmail.isEmpty()
                || !paymentProfiles.matchesAccountMerchant(accountEmail.get(), transaction.merchantId())) {
            counters.mismatched++;
            log.warn("payment_reconciliation_mismatch reason=order_or_environment orderNo={} transactionNo={}",
                    transaction.orderNo(), transaction.transactionNo());
            return;
        }

        CreditOrder order = optionalOrder.get();
        Optional<Payment> optionalPayment = paymentRepository.findByCreditOrder(order);
        if (optionalPayment.isEmpty()) {
            counters.mismatched++;
            log.warn("payment_reconciliation_mismatch reason=missing_payment orderNo={} transactionNo={}",
                    transaction.orderNo(), transaction.transactionNo());
            return;
        }
        Payment payment = optionalPayment.get();
        if (payment.getPgProvider() != PgProvider.HECTO
                || transaction.amount() != order.getPriceAmountSnapshot()) {
            counters.mismatched++;
            log.warn("payment_reconciliation_mismatch reason=provider_or_amount orderNo={} transactionNo={}",
                    transaction.orderNo(), transaction.transactionNo());
            return;
        }

        if (order.getOrderStatus() == OrderStatus.PAID) {
            if (payment.getPaymentStatus() == PaymentStatus.SUCCESS
                    && Integer.valueOf(transaction.amount()).equals(payment.getApprovedAmount())
                    && transaction.transactionNo().equals(payment.getPaymentKey())) {
                counters.matched++;
            } else {
                counters.mismatched++;
                log.warn("payment_reconciliation_mismatch reason=paid_state orderNo={} transactionNo={}",
                        transaction.orderNo(), transaction.transactionNo());
            }
            return;
        }

        if ((order.getOrderStatus() != OrderStatus.READY && order.getOrderStatus() != OrderStatus.EXPIRED)
                || (payment.getPaymentStatus() != PaymentStatus.READY
                && payment.getPaymentStatus() != PaymentStatus.EXPIRED)) {
            counters.mismatched++;
            log.warn("payment_reconciliation_mismatch reason=invalid_local_state orderNo={} transactionNo={}",
                    transaction.orderNo(), transaction.transactionNo());
            return;
        }

        if (!recover) {
            counters.mismatched++;
            log.warn("payment_reconciliation_recovery_candidate orderNo={} transactionNo={} environment={}",
                    transaction.orderNo(), transaction.transactionNo(), profile.environment());
            return;
        }

        HectoChargeResult result = creditService.confirmHectoCharge(recoveryResult(transaction));
        if (result.outcome() == HectoChargeResult.Outcome.COMPLETED
                || result.outcome() == HectoChargeResult.Outcome.RECOVERED) {
            counters.recovered++;
            paymentObservability.recordPaymentReconciliationRecovered(environmentName(profile.environment()));
            log.info("payment_reconciliation_recovered orderNo={} transactionNo={} previousStatus={}",
                    transaction.orderNo(), transaction.transactionNo(), result.previousStatus());
        } else if (result.outcome() == HectoChargeResult.Outcome.ALREADY_PROCESSED) {
            counters.matched++;
        } else {
            counters.mismatched++;
        }
    }

    private void reconcileCancellation(HectoReconciliationTransaction transaction, Counters counters) {
        Optional<CreditOrder> order = creditOrderRepository.findByOrderNo(transaction.orderNo());
        if (order.isPresent() && order.get().getOrderStatus() == OrderStatus.CANCELED) {
            counters.matched++;
            return;
        }
        counters.mismatched++;
        log.warn("payment_reconciliation_cancellation_requires_review orderNo={} transactionNo={} originalTransactionNo={}",
                transaction.orderNo(), transaction.transactionNo(), transaction.originalTransactionNo());
    }

    private void reconcileMissingProviderApprovals(
            LocalDate targetDate,
            HectoPaymentProfiles.PaymentProfile profile,
            Set<String> providerApprovalKeys,
            Counters counters
    ) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();
        List<Payment> localPayments = paymentRepository.findApprovedForReconciliation(
                PgProvider.HECTO, PaymentStatus.SUCCESS, start, end
        );
        for (Payment payment : localPayments) {
            String email = payment.getCreditOrder().getMember().getEmail();
            if (paymentProfiles.forAccount(email).environment() != profile.environment()) {
                continue;
            }
            if (payment.getPaymentKey() == null || !providerApprovalKeys.contains(payment.getPaymentKey())) {
                counters.mismatched++;
                log.warn("payment_reconciliation_mismatch reason=missing_provider_approval orderNo={} paymentId={}",
                        payment.getCreditOrder().getOrderNo(), payment.getId());
            }
        }
    }

    private Map<String, String> recoveryResult(HectoReconciliationTransaction transaction) {
        Map<String, String> result = new HashMap<>();
        result.put("mchtId", transaction.merchantId());
        result.put("mchtTrdNo", transaction.orderNo());
        result.put("trdNo", transaction.transactionNo());
        result.put("trdAmt", String.valueOf(transaction.amount()));
        result.put("outStatCd", "0021");
        result.put("reconciliation", "true");
        return result;
    }

    private String environmentName(HectoPaymentProfiles.Environment environment) {
        return environment.name().toLowerCase();
    }

    private static class Counters {
        int matched;
        int recovered;
        int mismatched;
        int cancellations;
    }

    public record ReconciliationSummary(
            Long runId,
            LocalDate targetDate,
            HectoPaymentProfiles.Environment environment,
            int providerCount,
            int matchedCount,
            int recoveredCount,
            int mismatchCount,
            int cancellationCount,
            boolean recoveryEnabled
    ) {}
}
