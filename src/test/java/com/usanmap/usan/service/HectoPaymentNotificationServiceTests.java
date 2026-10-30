package com.usanmap.usan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.entity.HectoPaymentNotification;
import com.usanmap.usan.repository.HectoPaymentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HectoPaymentNotificationServiceTests {

    @Mock private HectoPaymentNotificationRepository notificationRepository;

    @Test
    void persistsOnlyFieldsRequiredForVerificationAndPaymentProcessing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HectoPaymentNotificationService service =
                new HectoPaymentNotificationService(notificationRepository, objectMapper);
        Map<String, String> result = new HashMap<>();
        result.put("mchtTrdNo", "ORD-123");
        result.put("trdNo", "TRD-123");
        result.put("outStatCd", "0021");
        result.put("trdAmt", "1000");
        result.put("email", "customer@example.com");
        result.put("cardNo", "123456******7890");

        HectoPaymentNotificationService.AcceptanceResult acceptance = service.accept(result);

        ArgumentCaptor<HectoPaymentNotification> captor = ArgumentCaptor.forClass(HectoPaymentNotification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        Map<String, String> storedPayload = objectMapper.readValue(
                captor.getValue().getPayloadJson(),
                new TypeReference<>() {}
        );
        assertThat(storedPayload)
                .containsEntry("mchtTrdNo", "ORD-123")
                .containsEntry("trdNo", "TRD-123")
                .containsEntry("outStatCd", "0021")
                .containsEntry("trdAmt", "1000")
                .doesNotContainKeys("email", "cardNo");
        assertThat(acceptance.duplicate()).isFalse();
    }

    @Test
    void acceptsDuplicatePgTransactionAsIdempotentRedelivery() {
        HectoPaymentNotificationService service =
                new HectoPaymentNotificationService(notificationRepository, new ObjectMapper());
        Map<String, String> result = Map.of("mchtTrdNo", "ORD-123", "trdNo", "TRD-123");
        when(notificationRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate event key"));
        when(notificationRepository.existsByEventKey("TRD-123")).thenReturn(true);

        HectoPaymentNotificationService.AcceptanceResult acceptance = service.accept(result);

        verify(notificationRepository).existsByEventKey("TRD-123");
        assertThat(acceptance.duplicate()).isTrue();
    }
}
