package com.usanmap.usan.service;

import com.usanmap.usan.entity.*;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SmsListingShareTests {
    private final BrokerPropertyCountRepository brokers = mock(BrokerPropertyCountRepository.class);
    private final ListingSendService sends = mock(ListingSendService.class);
    private final Listing listing = mock(Listing.class);
    private final UUID code = UUID.randomUUID();

    private SmsService service(ExchangeFunction exchange) {
        SmsService sms = new SmsService(WebClient.builder().exchangeFunction(exchange).build(), brokers, sends);
        ReflectionTestUtils.setField(sms, "secretKey", "test-secret");
        ReflectionTestUtils.setField(sms, "accessKey", "test-key");
        ReflectionTestUtils.setField(sms, "serviceId", "test-service");
        ReflectionTestUtils.setField(sms, "fromNumber", "01000000000");
        ReflectionTestUtils.setField(sms, "baseUrl", "https://example.com");
        BrokerPropertyCount broker = mock(BrokerPropertyCount.class);
        when(broker.getPhone()).thenReturn("01012345678");
        when(brokers.findByBrokerCodeIn(List.of(code))).thenReturn(List.of(broker));
        when(listing.getTradeType()).thenReturn("SALE");
        when(sends.prepare(eq(listing), eq(1L), any())).thenReturn(8L);
        return sms;
    }

    @Test
    void recordsProviderRequestIdWithoutClaimingDeliverySuccess() {
        SmsService sms = service(request -> Mono.just(ClientResponse.create(HttpStatus.ACCEPTED)
                .header("Content-Type", "application/json").body("{\"requestId\":\"req-8\"}").build()));
        sms.sendListingShare(List.of(code), listing, 1L);
        verify(sends).recordRequest(8L, "ACCEPTED", "req-8", null);
    }

    @Test
    void transportFailureRemainsUnknownAndIsReportedToCaller() {
        SmsService sms = service(request -> Mono.error(new RuntimeException("timeout")));
        assertThatThrownBy(() -> sms.sendListingShare(List.of(code), listing, 1L))
                .isInstanceOf(IllegalStateException.class);
        verify(sends).recordRequest(8L, "UNKNOWN", null, null);
        verify(sends, never()).recordRequest(eq(8L), eq("ACCEPTED"), any(), any());
    }

    @Test
    void providerRejectionIsRecordedAsFailed() {
        SmsService sms = service(request -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build()));
        assertThatThrownBy(() -> sms.sendListingShare(List.of(code), listing, 1L))
                .isInstanceOf(IllegalStateException.class);
        verify(sends).recordRequest(8L, "FAILED", null, "400");
    }

    @Test
    void missingReceiptRemainsUnknown() {
        SmsService sms = service(request -> Mono.just(ClientResponse.create(HttpStatus.ACCEPTED)
                .header("Content-Type", "application/json").body("{}").build()));
        assertThatThrownBy(() -> sms.sendListingShare(List.of(code), listing, 1L))
                .isInstanceOf(IllegalStateException.class);
        verify(sends).recordRequest(8L, "UNKNOWN", null, null);
    }
}
