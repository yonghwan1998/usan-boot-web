package com.usanmap.usan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.entity.HectoPaymentNotification;
import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HectoPaymentNotificationService {

    private static final Set<String> STORED_FIELDS = Set.of(
            "outStatCd",
            "outRsltCd",
            "outRsltMsg",
            "trdNo",
            "method",
            "bizType",
            "mchtId",
            "mchtTrdNo",
            "trdDtm",
            "trdAmt",
            "pktHash"
    );

    private final HectoPaymentNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /** 검증을 마친 노티를 처리 큐에 영속화한다. 같은 거래번호의 재통보는 성공으로 간주한다. */
    public void accept(Map<String, String> result) {
        String orderNo = required(result, "mchtTrdNo");
        String transactionNo = required(result, "trdNo");
        String payloadJson = toJson(onlyRequiredFields(result));
        LocalDateTime now = LocalDateTime.now();

        HectoPaymentNotification notification = HectoPaymentNotification.builder()
                .eventKey(transactionNo)
                .orderNo(orderNo)
                .transactionNo(transactionNo)
                .payloadJson(payloadJson)
                .status(PaymentNotificationStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(now)
                .receivedAt(now)
                .updatedAt(now)
                .build();

        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            if (!notificationRepository.existsByEventKey(transactionNo)) {
                throw e;
            }
        }
    }

    private String required(Map<String, String> result, String key) {
        String value = result.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("필수 결제 노티 값이 없습니다: " + key);
        }
        return value;
    }

    private Map<String, String> onlyRequiredFields(Map<String, String> result) {
        Map<String, String> filtered = new LinkedHashMap<>();
        result.forEach((key, value) -> {
            if (STORED_FIELDS.contains(key)) {
                filtered.put(key, value);
            }
        });
        return filtered;
    }

    private String toJson(Map<String, String> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("결제 노티를 저장 형식으로 변환하지 못했습니다.", e);
        }
    }
}
