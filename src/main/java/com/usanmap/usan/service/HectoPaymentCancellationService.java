package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class HectoPaymentCancellationService {

    private final HectoPaymentCancellationTransactionService transactionService;
    private final HectoCancellationClient cancellationClient;
    private final PaymentObservability observability;
    private final boolean enabled;
    private final ZoneId zoneId;

    public HectoPaymentCancellationService(
            HectoPaymentCancellationTransactionService transactionService,
            HectoCancellationClient cancellationClient,
            PaymentObservability observability,
            @Value("${hecto.cancellation.enabled:false}") boolean enabled,
            @Value("${hecto.cancellation.zone:Asia/Seoul}") String zone
    ) {
        this.transactionService = transactionService;
        this.cancellationClient = cancellationClient;
        this.observability = observability;
        this.enabled = enabled;
        this.zoneId = ZoneId.of(zone);
    }

    public CancellationResult cancelFull(Long paymentId, String reason) {
        if (!enabled) {
            throw new IllegalStateException("헥토 결제 취소 기능이 비활성화되어 있습니다.");
        }

        LocalDateTime requestedAt = LocalDateTime.now(zoneId);
        HectoPaymentCancellationTransactionService.PreparedCancellation prepared =
                transactionService.prepare(paymentId, reason, requestedAt);
        if (prepared.alreadySucceeded()) {
            return new CancellationResult(prepared.cancellationId(), PaymentCancellationStatus.SUCCEEDED,
                    "이미 취소된 결제입니다.");
        }

        transactionService.markProcessing(prepared.cancellationId());
        HectoCancellationClient.CancellationCommand command = new HectoCancellationClient.CancellationCommand(
                prepared.cancellationNo(), prepared.originalTransactionNo(), prepared.amount(),
                prepared.reason(), prepared.requestedAt()
        );

        HectoCancellationClient.CancellationResponse response;
        try {
            response = cancellationClient.cancel(prepared.profile(), command);
        } catch (Exception e) {
            transactionService.requireReview(prepared.cancellationId(),
                    "PG 취소 응답을 확정할 수 없습니다: " + e.getClass().getSimpleName());
            observability.recordPaymentCancellation(prepared.profile().environment().name(), "review_required");
            log.error("payment_cancellation_review_required cancellationId={} environment={} reason={}",
                    prepared.cancellationId(), prepared.profile().environment(), e.getClass().getSimpleName());
            return new CancellationResult(prepared.cancellationId(), PaymentCancellationStatus.REVIEW_REQUIRED,
                    "PG 취소 결과를 거래대사 또는 헥토파이낸셜에서 확인해야 합니다.");
        }

        if (!response.success()) {
            transactionService.failAndRelease(prepared.cancellationId(), response);
            observability.recordPaymentCancellation(prepared.profile().environment().name(), "failed");
            log.warn("payment_cancellation_failed cancellationId={} environment={} resultCode={}",
                    prepared.cancellationId(), prepared.profile().environment(), response.resultCode());
            return new CancellationResult(prepared.cancellationId(), PaymentCancellationStatus.FAILED,
                    response.resultMessage());
        }

        try {
            transactionService.complete(prepared.cancellationId(), response);
        } catch (Exception e) {
            transactionService.requireReview(prepared.cancellationId(),
                    "PG 취소 성공 후 로컬 반영을 확인해야 합니다: " + e.getClass().getSimpleName());
            observability.recordPaymentCancellation(prepared.profile().environment().name(), "review_required");
            log.error("payment_cancellation_local_finalize_failed cancellationId={} environment={} reason={}",
                    prepared.cancellationId(), prepared.profile().environment(), e.getClass().getSimpleName());
            return new CancellationResult(prepared.cancellationId(), PaymentCancellationStatus.REVIEW_REQUIRED,
                    "PG 취소는 성공했지만 로컬 상태 반영을 확인해야 합니다.");
        }

        observability.recordPaymentCancellation(prepared.profile().environment().name(), "succeeded");
        log.info("payment_cancellation_succeeded cancellationId={} environment={}",
                prepared.cancellationId(), prepared.profile().environment());
        return new CancellationResult(prepared.cancellationId(), PaymentCancellationStatus.SUCCEEDED,
                "결제 전액 취소가 완료되었습니다.");
    }

    public record CancellationResult(Long cancellationId, PaymentCancellationStatus status, String message) {}
}
