package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.CreditService;
import com.usanmap.usan.service.PaymentObservability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditApiControllerTests {

    @Mock private CreditService creditService;
    @Mock private SecurityUtils securityUtils;
    @Mock private PaymentObservability paymentObservability;

    @InjectMocks private CreditApiController controller;

    @Test
    void createsOrderForAuthenticatedMemberAndRecordsMetric() {
        when(securityUtils.currentUserIdOrThrow()).thenReturn(1L);
        when(creditService.createOrder(1L, 10L)).thenReturn("ORD-123");

        ResponseEntity<Map<String, String>> response = controller.createOrder(10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("orderNo", "ORD-123");
        verify(paymentObservability).recordPaymentOrderCreated("hecto");
    }
}
