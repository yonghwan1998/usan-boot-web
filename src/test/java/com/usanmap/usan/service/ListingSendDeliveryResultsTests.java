package com.usanmap.usan.service;

import com.usanmap.usan.dto.SmsDeliveryResult;
import com.usanmap.usan.entity.*;
import com.usanmap.usan.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingSendDeliveryResultsTests {
    @Mock ListingSendBatchRepository batches;
    @Mock ListingSendHistoryRepository histories;
    @Mock CreditService credits;
    @Mock ListingService listings;
    @InjectMocks ListingSendService service;
    private final LocalDateTime time = LocalDateTime.of(2026,11,20,10,0);

    private void prepare(List<ListingSendHistory> rows) {
        when(batches.findLockedById(1L)).thenReturn(Optional.of(ListingSendBatch.builder()
                .id(1L).userId(3L).providerRequestId("request-1").build()));
        when(histories.findAllByBatchIdOrderByIdAsc(1L)).thenReturn(rows);
    }

    @Test
    void appliesOnlyConfirmedResultsAndDoesNotRefundOrOverwriteFinalRows() {
        var success = row("010-1111-1111", "ACCEPTED", 0);
        var fail = row("01022222222", "UNKNOWN", 0);
        var pending = row("01033333333", "ACCEPTED", 0);
        var finalRow = row("01044444444", "DELIVERED", 0);
        var refunded = row("01055555555", "ACCEPTED", 1);
        prepare(List.of(success, fail, pending, finalRow, refunded));
        int updated = service.applyDeliveryResults(1L, "request-1", List.of(
                result("m1", "+821011111111", "COMPLETED", "success"),
                result("m2", "01022222222", "COMPLETED", "fail"),
                result("m3", "01033333333", "PROCESSING", "success"),
                result("m4", "01044444444", "COMPLETED", "fail"),
                result("m5", "01055555555", "COMPLETED", "success")));
        assertThat(updated).isEqualTo(2);
        assertThat(success.getDeliveryStatus()).isEqualTo("DELIVERED");
        assertThat(success.getProviderMessageId()).isEqualTo("m1");
        assertThat(success.getDeliveredAt()).isEqualTo(time);
        assertThat(fail.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(fail.getFailedAt()).isEqualTo(time);
        assertThat(fail.getRefundedCredit()).isZero();
        assertThat(pending.getDeliveryStatus()).isEqualTo("ACCEPTED");
        assertThat(finalRow.getDeliveryStatus()).isEqualTo("DELIVERED");
        assertThat(refunded.getDeliveryStatus()).isEqualTo("ACCEPTED");
        verifyNoInteractions(credits);
        assertThat(service.applyDeliveryResults(1L, "request-1", List.of(result("m1", "01011111111", "COMPLETED", "fail"))))
                .isZero();
        assertThat(success.getDeliveryStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void doesNotGuessMappingForDuplicateOrTestOverriddenRecipientPhones() {
        var first = row("01011111111", "ACCEPTED", 0);
        var second = row("01011111111", "ACCEPTED", 0);
        var third = row("01022222222", "ACCEPTED", 0);
        prepare(List.of(first, second, third));
        assertThat(service.applyDeliveryResults(1L, "request-1", List.of(
                result("m1", "01011111111", "COMPLETED", "success"),
                result("m2", "01011111111", "COMPLETED", "fail"),
                result("m3", "01033933402", "COMPLETED", "success")))).isZero();
        assertThat(first.getDeliveryStatus()).isEqualTo("ACCEPTED");
        assertThat(third.getDeliveryStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void ignoresResultsFromOtherRequest() {
        var row = row("01011111111", "ACCEPTED", 0);
        prepare(List.of(row));
        assertThat(service.applyDeliveryResults(1L, "request-1", List.of(new SmsDeliveryResult(
                "foreign-request", "m1", "01011111111", "COMPLETED", "success", "0", time)))).isZero();
    }

    private ListingSendHistory row(String phone, String status, int refund) {
        return ListingSendHistory.builder().batchId(1L).userId(3L).brokerPhone(phone).deliveryStatus(status)
                .refundedCredit(refund).build();
    }
    private SmsDeliveryResult result(String id, String phone, String status, String name) {
        return new SmsDeliveryResult("request-1", id, phone, status, name, "success".equals(name) ? "0" : "3018", time);
    }
}
