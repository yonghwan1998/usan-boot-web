package com.usanmap.usan.batch.job.broker;

import lombok.RequiredArgsConstructor;
import com.usanmap.usan.service.BrokerExcelIngestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @date    2025-11-18
 * @author  yongss
 *
 * 처리 과정:
 *  - 매일 1:30에 Job 실행
 *  - 특정 경로의 엑셀 전체 read
 *  - broker_info, listing_info 테이블에 나누어 UPSERT
 *  - 성공한 파일 제거
 *
 * 예외/주의:
 *  - broker_info 테이블은 중개사 이름과 중개등록번호를 기준으로 UPSERT
 *  - listing_info 테이블은 항상 INSERT
 */
@Component
@RequiredArgsConstructor
public class BrokerExcelScheduler {

    private final BrokerExcelIngestService brokerExcelIngestService;

//    @Scheduled(cron = "0 30 1 * * *", zone = "Asia/Seoul")
    public void run() throws Exception {
        brokerExcelIngestService.run();
    }
}
