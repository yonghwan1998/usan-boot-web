package com.usanmap.usan.batch.job.brokerproperty;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Configuration
public class BrokerPropertyCountJobConfig {

    public static final String JOB_NAME = "brokerPropertyCountJob";

    /**
     * @date    2025-11-18
     * @author  yongss
     *
     * 처리 과정:
     *  - brokerPropertyCountJob() 실행 시 첫 스텝으로 실행
     *  - tasklet(RebuildBrokerPropertyCountTasklet) 호출
     */
    @Bean
    public Step brokerPropertyCountStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        RebuildBrokerPropertyCountTasklet tasklet) {
        // The tasklet owns the connection and commits staging pages independently.
        var transactionAttribute = new DefaultTransactionAttribute();
        transactionAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return new StepBuilder("brokerPropertyCountStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .transactionAttribute(transactionAttribute)
                .build();
    }

    /**
     * @date    2025-11-18
     * @author  yongss
     *
     * 처리 과정:
     *  - BrokerPropertyCountScheduler.run() 시 실행
     *  - start()를 통해 Job에 등록되어 있는 Step(brokerPropertyCountStep) 실행
     */
    @Bean(name = JOB_NAME)
    public Job brokerPropertyCountJob(JobRepository jobRepository,
                                      Step brokerPropertyCountStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(brokerPropertyCountStep)
                .build();
    }
}
