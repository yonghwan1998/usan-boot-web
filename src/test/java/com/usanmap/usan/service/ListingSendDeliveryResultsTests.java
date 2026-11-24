package com.usanmap.usan.service;

import com.usanmap.usan.dto.SmsDeliveryResult;
import com.usanmap.usan.entity.*;
import com.usanmap.usan.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
    void refundsConfirmedFailureWithoutOverwritingFinalRows() {
        var success = row("010-1111-1111", "ACCEPTED", 0);
        var fail = row("01022222222", "UNKNOWN", 0);
        var pending = row("01033333333", "ACCEPTED", 0);
        var finalRow = row("01044444444", "DELIVERED", 0);
        var refunded = row("01055555555", "ACCEPTED", 1);
        prepare(List.of(success, fail, pending, finalRow, refunded));
        org.springframework.test.util.ReflectionTestUtils.setField(fail, "id", 10L);
        when(credits.refundForFailedShare(any(), eq(10L)))
                .thenReturn(CreditLedger.builder().id(20L).build());
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
        assertThat(fail.getRefundedCredit()).isEqualTo(1);
        assertThat(pending.getDeliveryStatus()).isEqualTo("ACCEPTED");
        assertThat(finalRow.getDeliveryStatus()).isEqualTo("DELIVERED");
        assertThat(refunded.getDeliveryStatus()).isEqualTo("ACCEPTED");
        verify(credits).refundForFailedShare(any(), eq(10L));
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

    @ParameterizedTest
    @ValueSource(strings = {"3006", "3001", "3003", "3004", "2000", "2003", "2007", "3008",
            "3012", "3013", "E916", "E917", "3011", "3015", "S000", "S001", "3017",
            "3018", "3023", "3009", "3022", "E903", "E904", "S999", "E999", "NEW_FAILURE"})
    void refundsConfirmedFailureOnceAndKeepsRefundAudit(String code) {
        var rejected = row("01011111111", "ACCEPTED", 0);
        org.springframework.test.util.ReflectionTestUtils.setField(rejected, "id", 10L);
        prepare(List.of(rejected));
        when(credits.refundForFailedShare(any(), eq(10L)))
                .thenReturn(CreditLedger.builder().id(20L).build());
        var result = new SmsDeliveryResult("request-1", "m1", "01011111111", "COMPLETED", "fail", code, time);

        assertThat(service.applyDeliveryResults(1L, "request-1", List.of(result, result))).isEqualTo(1);
        assertThat(rejected.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(rejected.getRefundedCredit()).isEqualTo(1);
        assertThat(rejected.getRefundCreditLedgerId()).isEqualTo(20L);
        assertThat(rejected.getRefundedAt()).isNotNull();
        assertThat(rejected.getSendStatusLabel()).isEqualTo("환불 처리");
        assertThat(service.applyDeliveryResults(1L, "request-1", List.of(result))).isZero();
        verify(credits, times(1)).refundForFailedShare(any(), eq(10L));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "0", "E915"})
    void recordsFailureButDoesNotRefundDuplicateOrInvalidCode(String code) {
        var failed = row("01011111111", "ACCEPTED", 0);
        prepare(List.of(failed));
        var result = new SmsDeliveryResult("request-1", "m1", "01011111111", "COMPLETED", "fail", code, time);
        assertThat(service.applyDeliveryResults(1L, "request-1", List.of(result))).isEqualTo(1);
        assertThat(failed.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(failed.getResultCode()).isEqualTo(code);
        assertThat(failed.getRefundedCredit()).isZero();
        assertThat(failed.getRefundCreditLedgerId()).isNull();
        verifyNoInteractions(credits);
    }

    @Test
    void doesNotRefundUnconfirmedOrMismatchedRejections() {
        var pending = row("01011111111", "ACCEPTED", 0);
        prepare(List.of(pending));
        for (var result : List.of(
                new SmsDeliveryResult("request-1", "m1", "01011111111", "PROCESSING", "fail", "3006", time),
                new SmsDeliveryResult("other", "m1", "01011111111", "COMPLETED", "fail", "3006", time),
                new SmsDeliveryResult("request-1", "m1", "01099999999", "COMPLETED", "fail", "3006", time))) {
            assertThat(service.applyDeliveryResults(1L, "request-1", List.of(result))).isZero();
        }
        assertThat(pending.getRefundedCredit()).isZero();
        verifyNoInteractions(credits);
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
