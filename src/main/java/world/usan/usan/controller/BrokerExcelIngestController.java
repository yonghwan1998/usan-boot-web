package world.usan.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import world.usan.usan.batch.job.broker.BrokerExcelIngestJobConfig;

/**
 * @date    2025-11-19
 * @author  yongss
 * @desc    brokerExcelIngestJob 수동 실행 전용 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/broker")
public class BrokerExcelIngestController {

    private final JobLauncher jobLauncher;
    @Qualifier(BrokerExcelIngestJobConfig.JOB_NAME)
    private final Job brokerExcelIngestJob;

    /**
     * @date    2025-11-19
     * @author  yongss
     *
     * 처리 과정:
     *  - /broker/batch/run-broker-excel-ingest POST 요청
     *  - brokerExcelIngestJob 수동 실행
     */
    @PostMapping("/batch/run-broker-excel-ingest")
    public ResponseEntity<String> run() throws Exception {

        var params = new JobParametersBuilder()
                .addLong("ts", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(brokerExcelIngestJob, params);

        return ResponseEntity.ok("brokerExcelIngestJob batch started");
    }
}
