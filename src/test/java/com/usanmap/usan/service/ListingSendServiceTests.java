package com.usanmap.usan.service;

import com.usanmap.usan.entity.*;
import com.usanmap.usan.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ListingSendServiceTests {
    @Mock ListingSendBatchRepository batches;
    @Mock ListingSendHistoryRepository histories;
    @Mock CreditService credits;
    @Mock ListingService listings;
    @InjectMocks ListingSendService service;

    @Test
    void preparesOneBatchWithCreditAndBrokerSnapshots() {
        Listing listing = mock(Listing.class);
        when(listing.getId()).thenReturn(9L);
        when(listing.getUserId()).thenReturn(1L);
        when(listing.getAddressName()).thenReturn("서울시 강남구 123");
        when(listing.getAddressDetail()).thenReturn("우산 아파트 101호");
        BrokerPropertyCount broker = mock(BrokerPropertyCount.class);
        UUID code = UUID.randomUUID();
        when(broker.getBrokerCode()).thenReturn(code);
        when(broker.getPhone()).thenReturn("01012345678");
        when(broker.getOfficeName()).thenReturn("우산 부동산");
        when(broker.getAddrRoad()).thenReturn("서울시 강남구 124");
        when(credits.deductForShare(1L, 1, 9L)).thenReturn(CreditLedger.builder()
                .id(12L).changeAmount(-1).balanceAfter(99).build());
        when(batches.save(any())).thenAnswer(inv -> {
            ListingSendBatch batch = inv.getArgument(0);
            ReflectionTestUtils.setField(batch, "id", 22L);
            return batch;
        });
        assertThat(service.prepare(listing, 1L, List.of(broker))).isEqualTo(22L);
        ArgumentCaptor<ListingSendBatch> batchCaptor = ArgumentCaptor.forClass(ListingSendBatch.class);
        verify(batches).save(batchCaptor.capture());
        ListingSendBatch batch = batchCaptor.getValue();
        assertThat(batch.getCreditBefore()).isEqualTo(100);
        assertThat(batch.getCreditAfter()).isEqualTo(99);
        assertThat(batch.getCreditLedgerId()).isEqualTo(12L);
        assertThat(batch.getListingTitle()).isEqualTo("우산 아파트 101호");
        assertThat(batch.getRequestStatus()).isEqualTo("REQUESTED");
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ListingSendHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(histories).saveAll(captor.capture());
        ListingSendHistory recipient = captor.getValue().get(0);
        assertThat(recipient.getBatchId()).isEqualTo(22L);
        assertThat(recipient.getBrokerCode()).isEqualTo(code);
        assertThat(recipient.getBrokerOfficeName()).isEqualTo("우산 부동산");
        assertThat(recipient.getSentAt()).isEqualTo(batch.getRequestedAt());
        assertThat(recipient.getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(recipient.getRefundedCredit()).isZero();
    }

    @Test
    void rejectsNonOwnerBeforeChargingCredit() {
        Listing listing = mock(Listing.class);
        when(listing.getUserId()).thenReturn(2L);
        assertThatThrownBy(() -> service.prepare(listing, 1L, List.of(mock(BrokerPropertyCount.class))))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(credits, batches, histories);
    }

    @Test
    void rejectsMissingOrForeignBatchWithoutReadingRecipients() {
        when(batches.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRecipients(1L, 5L, 0, "ALL")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(histories);
    }

    @Test
    void apiAcceptanceDoesNotBecomeDeliverySuccess() {
        ListingSendBatch batch = ListingSendBatch.builder().id(5L).build();
        ListingSendHistory history = ListingSendHistory.builder().deliveryStatus("PENDING").build();
        when(batches.findById(5L)).thenReturn(Optional.of(batch));
        when(histories.findAllByBatchIdOrderByIdAsc(5L)).thenReturn(List.of(history));
        service.recordRequest(5L, "ACCEPTED", "request-1", null);
        assertThat(batch.getProviderRequestId()).isEqualTo("request-1");
        assertThat(history.getDeliveryStatus()).isEqualTo("ACCEPTED");
        assertThat(history.getDeliveredAt()).isNull();
    }

    @Test
    void legacyHistoryUsesOwnerAndNullBatchFilter() {
        when(histories.findAllByUserIdAndBatchIdIsNullOrderBySentAtDescIdDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        service.getLegacy(1L, -1);
        verify(histories).findAllByUserIdAndBatchIdIsNullOrderBySentAtDescIdDesc(eq(1L),
                argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 50));
    }

    @Test
    void listingNumbersFollowMyListingsRatherThanGlobalIdsOrSendOrder() {
        when(listings.getMyListings(1L)).thenReturn(List.of(
                new com.usanmap.usan.dto.MyListingDto(900L, "aaa111", "주소1", null, null, null, null),
                new com.usanmap.usan.dto.MyListingDto(72L, "aaa222", "주소2", null, null, null, null)));
        assertThat(service.getListingLabels(1L)).containsEntry(900L, "매물 1").containsEntry(72L, "매물 2")
                .doesNotContainKey(1L);
        verify(listings).getMyListings(1L);
    }

    @Test
    void rejectsInvalidFilterBeforeReadingHistory() {
        assertThatThrownBy(() -> service.getRecipients(1L, 5L, 0, "OTHER"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(batches, histories);
    }
}
