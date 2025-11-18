package world.usan.usan.batch.job.broker;

import org.springframework.batch.core.*;
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
import world.usan.usan.batch.job.broker.processor.EnrichedBrokerItem;
import world.usan.usan.batch.job.broker.processor.NaverAddressEnrichProcessor;
import world.usan.usan.batch.job.broker.reader.BrokerMultiFileReader;
import world.usan.usan.batch.job.broker.reader.BrokerRow;
import world.usan.usan.batch.job.broker.support.FileMoveUtil;
import world.usan.usan.batch.job.broker.support.BatchExcelProps;
import world.usan.usan.batch.job.broker.writer.BrokerUpsertWriter;

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
    public BrokerMultiFileReader brokerMultiFileReader(BatchExcelProps props) {
        return new BrokerMultiFileReader(props);
    }

    /**
     * @date    2025-11-18
     * @author  yongss
     *
     * 처리 과정:
     *  - brokerExcelIngestJob() 실행 시 첫 스텝으로 실행
     *  - reader(BrokerMultiFileReader) 호출
     *  - 리턴된 값을 processor(NaverAddressEnrichProcessor)에 넘김
     *  - processor 결과를 모아서 writer(BrokerUpsertWriter) 호출
     */
    @Bean
    public Step brokerExcelStep(JobRepository jobRepository,
                                PlatformTransactionManager tx,
                                BrokerMultiFileReader reader,
                                NaverAddressEnrichProcessor processor,
                                BrokerUpsertWriter writer) {
        return new StepBuilder("brokerExcelStep", jobRepository)
                .<BrokerRow, EnrichedBrokerItem>chunk(chunkSize, tx)
                .reader(new SynchronizedItemStreamReaderBuilder<BrokerRow>()
                        .delegate(reader)
                        .build())
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(IllegalStateException.class)
                .skipLimit(Integer.MAX_VALUE)
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
     *  - Step 처리 후 afterJob으로 FileMoveUtil.moveAllToArchive() 실행
     *  - 완료한 엑셀 파일 move
     */
    @Bean(name = JOB_NAME)
    public Job brokerExcelIngestJob(JobRepository jobRepository,
                                    Step brokerExcelStep,
                                    FileMoveUtil fileMoveUtil) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(brokerExcelStep)
                .listener(new JobExecutionListenerSupport() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            fileMoveUtil.moveAllToArchive();
                        }
                    }
                })
                .build();
    }
}
