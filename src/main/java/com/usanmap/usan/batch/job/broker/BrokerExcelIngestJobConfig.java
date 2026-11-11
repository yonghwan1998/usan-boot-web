package com.usanmap.usan.batch.job.broker;

import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;
import com.usanmap.usan.batch.job.broker.support.FailedExcelHandler;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import com.usanmap.usan.batch.job.broker.processor.EnrichedBrokerItem;
import com.usanmap.usan.batch.job.broker.processor.NaverAddressEnrichProcessor;
import com.usanmap.usan.batch.job.broker.reader.SaxExcelRowItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.core.io.FileSystemResource;
import com.usanmap.usan.batch.job.broker.reader.BrokerRow;
import com.usanmap.usan.batch.job.broker.support.BrokerErrorCollector;
import com.usanmap.usan.batch.job.broker.support.ErrorExcelWriter;
import com.usanmap.usan.batch.job.broker.support.FileMoveUtil;
import com.usanmap.usan.batch.job.broker.support.BatchExcelProps;
import com.usanmap.usan.batch.job.broker.writer.BrokerUpsertWriter;

/**
 * @date    2025-11-18
 * @author  yongss
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(BatchExcelProps.class)
public class BrokerExcelIngestJobConfig {

    public static final String JOB_NAME = "brokerExcelIngestJob";

    @Value("${usan.batch.chunk-size}")
    private int chunkSize;

    @Bean
    @StepScope
    public SaxExcelRowItemReader brokerExcelReader(BatchExcelProps props,
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        SaxExcelRowItemReader reader = new SaxExcelRowItemReader(props);
        reader.setResource(new FileSystemResource(inputFile));
        return reader;
    }

    /**
     * @date    2025-11-18
     * @author  yongss
     *
     * 처리 과정:
     *  - brokerExcelIngestJob() 실행 시 첫 스텝으로 실행
     *  - reader(SaxExcelRowItemReader) 호출
     *  - 리턴된 값을 processor(NaverAddressEnrichProcessor)에 넘김
     *  - processor 결과를 모아서 writer(BrokerUpsertWriter) 호출
     */
    @Bean
    public Step brokerExcelStep(JobRepository jobRepository,
                                PlatformTransactionManager tx,
                                SaxExcelRowItemReader reader,
                                NaverAddressEnrichProcessor processor,
                                BrokerUpsertWriter writer,
                                BrokerErrorCollector errorCollector,
                                ErrorExcelWriter errorExcelWriter,
                                BatchExcelProps excelProps) {
        return new StepBuilder("brokerExcelStep", jobRepository)
                .<BrokerRow, EnrichedBrokerItem>chunk(chunkSize, tx)
                .reader(new SynchronizedItemStreamReaderBuilder<BrokerRow>()
                        .delegate(reader)
                        .build())
                .processor(processor)
                .writer(writer)
                .listener(new ChunkListener() {
                    @Override
                    public void afterChunk(ChunkContext context) {
                        var executionContext = context.getStepContext().getStepExecution().getExecutionContext();
                        executionContext.putInt(FailedExcelHandler.LAST_COMMITTED_ROW,
                                executionContext.getInt(SaxExcelRowItemReader.LAST_READ_EXCEL_ROW, 0));
                    }
                })
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        errorCollector.clear();
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        var errors = errorCollector.snapshot();
                        if (!errors.isEmpty()) {
                            errorExcelWriter.write(excelProps.getError(), errors);
                        }
                        return stepExecution.getExitStatus();
                    }
                })
                .exceptionHandler((context, throwable) -> {throw throwable;})
                .build();
    }

    /**
     * @date    2025-11-18
     * @author  yongss
     *
     * 처리 과정:
     *  - BrokerExcelScheduler.run() 시 실행
     *  - start()를 통해 Job에 등록되어 있는 Step(brokerExcelStep) 실행
     *  - Step 처리 후 afterJob으로 FileMoveUtil.moveToArchive() 실행
     *  - 완료한 엑셀 파일 move
     */
    @Bean(name = JOB_NAME)
    public Job brokerExcelIngestJob(JobRepository jobRepository,
                                    Step brokerExcelStep,
                                    FileMoveUtil fileMoveUtil,
                                    FailedExcelHandler failedExcelHandler) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(brokerExcelStep)
                .listener(new JobExecutionListenerSupport() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            fileMoveUtil.moveToArchive(jobExecution.getJobParameters().getString("inputFile"));
                        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                            failedExcelHandler.handle(jobExecution);
                        }
                    }
                })
                .build();
    }
}
