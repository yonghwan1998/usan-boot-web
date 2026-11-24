package com.usanmap.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.usanmap.usan.batch.job.brokerproperty.BrokerPropertyCountJobConfig;

/**
 * @date    2025-11-19
 * @author  yongss
 * @desc    brokerPropertyCountJob 수동 실행 전용 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/broker")
public class BrokerPropertyCountController {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    @Qualifier(BrokerPropertyCountJobConfig.JOB_NAME)
    private final Job brokerPropertyCountJob;

    /**
     * @date    2025-11-19
     * @author  yongss
     *
     * 처리 과정:
     *  - /broker/batch/run-broker-property-count POST 요청
     *  - brokerPropertyCountJob 수동 실행
     */
    @PostMapping("/batch/run-broker-property-count")
    public ResponseEntity<String> run() throws Exception{

        if (!jobExplorer.findRunningJobExecutions(BrokerPropertyCountJobConfig.JOB_NAME).isEmpty()) {
            return ResponseEntity.status(409).body("brokerPropertyCount batch already running");
        }

        var params = new JobParametersBuilder()
                .addLong("ts",  System.currentTimeMillis())
                .toJobParameters();

        var execution = jobLauncher.run(brokerPropertyCountJob, params);
        if (execution.getStatus() == BatchStatus.FAILED) {
            return ResponseEntity.internalServerError().body("brokerPropertyCount batch failed");
        }

        return ResponseEntity.ok("brokerPropertyCount batch started");
    }
}
