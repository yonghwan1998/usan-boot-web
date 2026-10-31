package com.usanmap.usan.controller;

import com.usanmap.usan.entity.enums.PaymentCancellationStatus;
import com.usanmap.usan.service.HectoPaymentCancellationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/payments")
@RequiredArgsConstructor
public class PaymentCancellationAdminController {

    private final HectoPaymentCancellationService cancellationService;

    @PostMapping("/{paymentId}/cancellations")
    public ResponseEntity<HectoPaymentCancellationService.CancellationResult> cancel(
            @PathVariable Long paymentId,
            @Valid @RequestBody CancellationRequest request
    ) {
        HectoPaymentCancellationService.CancellationResult result =
                cancellationService.cancelFull(paymentId, request.reason());
        HttpStatus status = switch (result.status()) {
            case SUCCEEDED -> HttpStatus.OK;
            case FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case REVIEW_REQUIRED -> HttpStatus.ACCEPTED;
            default -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(result);
    }

    public record CancellationRequest(
            @NotBlank(message = "취소 사유를 입력해주세요.")
            @Size(max = 255, message = "취소 사유는 255자 이하여야 합니다.")
            String reason
    ) {}
}
