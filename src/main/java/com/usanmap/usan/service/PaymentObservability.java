package com.usanmap.usan.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PaymentObservability {

    private static final String NOTIFICATION_RECEIVED = "usan.payment.notification.received";
    private static final String NOTIFICATION_PROCESSED = "usan.payment.notification.processed";
    private static final String NOTIFICATION_RETRY = "usan.payment.notification.retry";
    private static final String NOTIFICATION_DURATION = "usan.payment.notification.processing.duration";
    private static final String PAYMENT_EXPIRED = "usan.payment.expired";
    private static final String PAYMENT_STATUS_TRANSITION = "usan.payment.status.transition";
    private static final String PAYMENT_ORDER_CREATED = "usan.payment.order.created";

    private final MeterRegistry meterRegistry;

    public void recordNotificationReceived(String outcome) {
        meterRegistry.counter(NOTIFICATION_RECEIVED, "outcome", outcome).increment();
    }

    public void recordNotificationProcessed(String outcome, Duration duration) {
        meterRegistry.counter(NOTIFICATION_PROCESSED, "outcome", outcome).increment();
        meterRegistry.timer(NOTIFICATION_DURATION, "outcome", outcome).record(duration);
    }

    public void recordNotificationRetry(String outcome) {
        meterRegistry.counter(NOTIFICATION_RETRY, "outcome", outcome).increment();
    }

    public void recordPaymentExpired() {
        meterRegistry.counter(PAYMENT_EXPIRED).increment();
    }

    public void recordPaymentStatusTransition(String source, String from, String to) {
        meterRegistry.counter(
                PAYMENT_STATUS_TRANSITION,
                "source", source,
                "from", from,
                "to", to
        ).increment();
    }

    public void recordPaymentOrderCreated(String provider) {
        meterRegistry.counter(PAYMENT_ORDER_CREATED, "provider", provider).increment();
    }
}
