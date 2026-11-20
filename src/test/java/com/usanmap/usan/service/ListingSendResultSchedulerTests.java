package com.usanmap.usan.service;

import com.usanmap.usan.entity.ListingSendBatch;
import com.usanmap.usan.repository.ListingSendBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ListingSendResultSchedulerTests {
    @Mock ListingSendBatchRepository batches;
    @Mock SmsService sms;
    @Mock ListingSendService sends;
    @InjectMocks ListingSendResultScheduler scheduler;

    @Test
    void selectsTenMinuteWindowAndProcessesEachRequestOnce() throws Exception {
        when(batches.findPendingResultBatches(any(), any(), eq(0L), any())).thenReturn(List.of(batch(1L, 2)));
        when(sms.getListingSendResults("request-1")).thenReturn(List.of());
        scheduler.pollPendingResults();
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> until = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(batches).findPendingResultBatches(from.capture(), until.capture(), eq(0L), any());
        assertThat(from.getValue()).isEqualTo(until.getValue().minusMinutes(10));
        verify(sms).getListingSendResults("request-1");
        verify(sends).applyDeliveryResults(1L, "request-1", List.of());
        var scheduled = ListingSendResultScheduler.class.getMethod("pollPendingResults")
                .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("0 * * * * *");
    }

    @Test
    void skipsBatchThatAgesOutBeforeNetworkCallAndContinuesAfterApiFailure() {
        when(batches.findPendingResultBatches(any(), any(), eq(0L), any()))
                .thenReturn(List.of(batch(1L, 11), batch(2L, 2), batch(3L, 1)));
        when(sms.getListingSendResults("request-2")).thenThrow(new IllegalStateException("timeout"));
        when(sms.getListingSendResults("request-3")).thenReturn(List.of());
        scheduler.pollPendingResults();
        verify(sms, never()).getListingSendResults("request-1");
        verify(sends, never()).applyDeliveryResults(eq(2L), any(), any());
        verify(sends).applyDeliveryResults(3L, "request-3", List.of());
    }

    @Test
    void keysetPagingDoesNotSkipBatchesWhenEarlierRowsBecomeFinal() {
        var first = java.util.stream.LongStream.rangeClosed(1, 100).mapToObj(id -> batch(id, 1)).toList();
        when(batches.findPendingResultBatches(any(), any(), eq(0L), any())).thenReturn(first);
        when(batches.findPendingResultBatches(any(), any(), eq(100L), any())).thenReturn(List.of(batch(101L, 1)));
        when(sms.getListingSendResults(any())).thenReturn(List.of());
        scheduler.pollPendingResults();
        verify(sends).applyDeliveryResults(101L, "request-101", List.of());
        verify(sms, times(101)).getListingSendResults(any());
    }

    private ListingSendBatch batch(Long id, int ageMinutes) {
        return ListingSendBatch.builder().id(id).requestedAt(LocalDateTime.now().minusMinutes(ageMinutes))
                .providerRequestId("request-" + id).build();
    }
}
