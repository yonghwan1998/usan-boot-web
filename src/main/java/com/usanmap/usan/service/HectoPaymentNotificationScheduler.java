package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.PaymentNotificationStatus;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HectoPaymentNotificationScheduler {

    private final HectoPaymentNotificationRepository notificationRepository;
    private final HectoPaymentNotificationProcessor notificationProcessor;

    @Value("${hecto.notification.batch-size:20}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${hecto.notification.processing-delay-ms:1000}",
            initialDelayString = "${hecto.notification.initial-delay-ms:1000}"
    )
    public void processPendingNotifications() {
        List<Long> notificationIds = notificationRepository.findDueIds(
                PaymentNotificationStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (Long notificationId : notificationIds) {
            try {
                notificationProcessor.process(notificationId);
            } catch (Exception e) {
                log.warn("헥토 결제 노티 처리 실패. notificationId={}", notificationId, e);
                try {
                    notificationProcessor.recordFailure(notificationId, e);
                } catch (Exception recordFailureException) {
                    log.error("헥토 결제 노티 재시도 기록 실패. notificationId={}", notificationId, recordFailureException);
                }
            }
        }
    }
}
