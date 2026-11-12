package com.usanmap.usan.service;

import com.usanmap.usan.batch.job.broker.BrokerExcelIngestJobConfig;
import com.usanmap.usan.batch.job.broker.support.BatchExcelProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerExcelIngestService {
    private final JobLauncher jobLauncher;
    @Qualifier(BrokerExcelIngestJobConfig.JOB_NAME)
    private final Job brokerExcelIngestJob;
    private final BatchExcelProps props;

    // 수동 실행과 스케줄 실행이 같은 파일을 동시에 처리하지 않도록 직렬화한다.
    public synchronized void run() throws Exception {
        try (var paths = Files.list(Path.of(props.getInbox()))) {
            var files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".xlsx"))
                    .sorted().toList();
            var splitter = new com.usanmap.usan.batch.job.broker.support.BrokerExcelSplitter(props);
            for (Path source : files) {
                java.util.List<Path> prepared;
                try {
                    prepared = splitter.prepare(source);
                } catch (Exception e) {
                    var failed = new org.springframework.batch.core.JobExecution(0L,
                            new JobParametersBuilder().addString("inputFile", source.toAbsolutePath().toString()).toJobParameters());
                    failed.addFailureException(e);
                    new com.usanmap.usan.batch.job.broker.support.FailedExcelHandler(props).handle(failed);
                    if (Files.exists(source)) throw new IllegalStateException("Failed to isolate unsplittable file: " + source, e);
                    log.warn("Excel split failed; isolated file and continuing: {}", source, e);
                    continue;
                }
                for (Path file : prepared) {
                    var params = new JobParametersBuilder()
                            .addString("inputFile", file.toAbsolutePath().toString())
                            .addString("runId", UUID.randomUUID().toString())
                            .toJobParameters();
                    var execution = jobLauncher.run(brokerExcelIngestJob, params);
                    if (execution.getStatus() == BatchStatus.FAILED && Files.notExists(file)) {
                        // afterJob에서 실패 파일을 격리한 경우 다음 파일을 계속 처리한다.
                        log.warn("Excel batch failed; continuing with next file: {}, executionId={}",
                                file, execution.getId());
                        continue;
                    }
                    if (execution.getStatus() != BatchStatus.COMPLETED) {
                        throw new IllegalStateException("Excel batch did not complete: " + file
                                + ", status=" + execution.getStatus());
                    }
                }
            }
        }
    }
}
