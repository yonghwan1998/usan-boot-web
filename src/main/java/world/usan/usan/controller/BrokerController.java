package world.usan.usan.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import world.usan.usan.batch.job.broker.BrokerExcelIngestJobConfig;
import world.usan.usan.batch.job.broker.support.BatchExcelProps;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/broker")
public class BrokerController {

    private final BatchExcelProps props;
    private final JobLauncher jobLauncher;
    private final Job brokerExcelIngestJob;

    public BrokerController(BatchExcelProps props,
                            JobLauncher jobLauncher,
                            @Qualifier(BrokerExcelIngestJobConfig.JOB_NAME) Job brokerExcelIngestJob) {
        this.props = props;
        this.jobLauncher = jobLauncher;
        this.brokerExcelIngestJob = brokerExcelIngestJob;
    }

    /**
     * @date    2025-11-18
     * @author  yongss
     * @param   file
     *
     * 처리 과정:
     *  - /broker/excel/upload POST 요청
     *  - inbox 경로로 파일 업로드
     */
    @PostMapping("/excel/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file")MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String name = StringUtils.cleanPath(file.getOriginalFilename());
        File dest = new File(props.getInbox(), name);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        return ResponseEntity.ok("uploaded: " + name);
    }

    /**
     * @date    2025-11-18
     * @author  yongss
     *
     * 처리 과정:
     *  - /broker/batch/run-batch POST 요청
     *  - brokerExcelIngestJob 수동 실행
     */
    @PostMapping("/batch/run-batch")
    public ResponseEntity<String> runBatch() throws Exception {

        var params = new JobParametersBuilder()
                .addLong("ts", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(brokerExcelIngestJob, params);

        return ResponseEntity.ok("brokerExcelIngestJob batch started");
    }
}
