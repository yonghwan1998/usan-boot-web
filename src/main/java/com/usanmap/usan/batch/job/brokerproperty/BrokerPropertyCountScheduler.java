package com.usanmap.usan.batch.job.brokerproperty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @date    2025-11-18
 * @author  yongss
 * @param
 * @return
 *
 * 처리 과정:
 *  - 매일 06:30에 Job 실행
 *  - broker_count_property 테이블 DELETE
 *  - 새로운 로우 INSERT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerPropertyCountScheduler {

    private final JobExplorer jobExplorer;

    @Qualifier(BrokerPropertyCountJobConfig.JOB_NAME)
    private final Job brokerPropertyCountJob;
    private final JobLauncher jobLauncher;

    @Scheduled(cron = "0 30 6 * * *", zone = "Asia/Seoul")
    public void run() throws Exception {

        if (!jobExplorer.findRunningJobExecutions(BrokerPropertyCountJobConfig.JOB_NAME).isEmpty()) {
            log.info("[BrokerPropertyCountScheduler] job already running, skip");
            return;
        }

        var params = new JobParametersBuilder()
                .addLong("ts", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(brokerPropertyCountJob, params);
    }
}
