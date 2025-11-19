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
import world.usan.usan.batch.job.brokerproperty.BrokerPropertyCountJobConfig;

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

        var params = new JobParametersBuilder()
                .addLong("ts",  System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(brokerPropertyCountJob, params);

        return ResponseEntity.ok("brokerPropertyCount batch started");
    }
}
