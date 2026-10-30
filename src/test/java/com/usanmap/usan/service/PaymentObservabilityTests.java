package com.usanmap.usan.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentObservabilityTests {

    @Test
    void recordsLowCardinalityPaymentMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PaymentObservability observability = new PaymentObservability(meterRegistry);

        observability.recordNotificationReceived("accepted");
        observability.recordNotificationProcessed("completed", Duration.ofMillis(25));
        observability.recordNotificationRetry("scheduled");
        observability.recordPaymentExpired();
        observability.recordPaymentStatusTransition("notification", "READY", "PAID");
        observability.recordPaymentOrderCreated("hecto");

        assertThat(meterRegistry.get("usan.payment.notification.received")
                .tag("outcome", "accepted").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("usan.payment.notification.processed")
                .tag("outcome", "completed").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("usan.payment.notification.processing.duration")
                .tag("outcome", "completed").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(25.0);
        assertThat(meterRegistry.get("usan.payment.notification.retry")
                .tag("outcome", "scheduled").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("usan.payment.expired").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("usan.payment.status.transition")
                .tags("source", "notification", "from", "READY", "to", "PAID")
                .counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("usan.payment.order.created")
                .tag("provider", "hecto").counter().count()).isEqualTo(1.0);
    }
}
