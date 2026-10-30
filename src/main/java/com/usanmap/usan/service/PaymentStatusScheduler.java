package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditOrderRepository;
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
public class PaymentStatusScheduler {

    private final CreditOrderRepository creditOrderRepository;
    private final PaymentStatusService paymentStatusService;

    @Value("${hecto.payment.ready-expiration-minutes:30}")
    private long readyExpirationMinutes;

    @Value("${hecto.payment.expiration-batch-size:100}")
    private int expirationBatchSize;

    @Scheduled(
            fixedDelayString = "${hecto.payment.expiration-delay-ms:60000}",
            initialDelayString = "${hecto.payment.expiration-initial-delay-ms:60000}"
    )
    public void expireStaleHectoOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(readyExpirationMinutes);
        List<Long> orderIds = creditOrderRepository.findStaleHectoOrderIds(
                OrderStatus.READY,
                cutoff,
                PgProvider.HECTO,
                PaymentStatus.READY,
                PageRequest.of(0, expirationBatchSize)
        );

        for (Long orderId : orderIds) {
            try {
                paymentStatusService.expireHectoOrder(orderId, cutoff);
            } catch (Exception e) {
                log.warn("헥토 결제 미완료 상태 전환 실패. orderId={}", orderId, e);
            }
        }
    }
}
