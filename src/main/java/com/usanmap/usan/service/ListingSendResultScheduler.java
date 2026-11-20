package com.usanmap.usan.service;

import com.usanmap.usan.entity.ListingSendBatch;
import com.usanmap.usan.repository.ListingSendBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingSendResultScheduler {
    private static final int PAGE_SIZE = 100;
    private final ListingSendBatchRepository batchRepository;
    private final SmsService smsService;
    private final ListingSendService listingSendService;

    @Scheduled(cron = "0 * * * * *")
    public void pollPendingResults() {
        LocalDateTime until = LocalDateTime.now();
        LocalDateTime from = until.minusMinutes(10);
        long afterId = 0L;
        List<ListingSendBatch> batches;
        do {
            batches = batchRepository.findPendingResultBatches(from, until, afterId, PageRequest.of(0, PAGE_SIZE));
            for (ListingSendBatch batch : batches) {
                afterId = batch.getId();
                // 앞선 요청 처리가 길어져 조회 시점에 10분을 넘긴 건도 제외한다.
                if (batch.getRequestedAt().isBefore(LocalDateTime.now().minusMinutes(10))) continue;
                try {
                    var results = smsService.getListingSendResults(batch.getProviderRequestId());
                    int updated = listingSendService.applyDeliveryResults(batch.getId(), batch.getProviderRequestId(), results);
                    log.info("listing_send_results_updated batchId={} updated={}", batch.getId(), updated);
                } catch (Exception e) {
                    // 조회 장애는 발송 실패로 바꾸지 않고 다음 분에 다시 확인한다.
                    log.warn("listing_send_results_poll_failed batchId={} exceptionType={}",
                            batch.getId(), e.getClass().getSimpleName());
                }
            }
        } while (batches.size() == PAGE_SIZE);
    }
}
