package com.doumi.donation.ranking.config;

import com.doumi.donation.ranking.model.dto.MemberRankingDto;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MonthlyRankingBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory;

    @Autowired
    public MonthlyRankingBatchConfig(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     SqlSessionFactory sqlSessionFactory) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Bean
    public Job monthlyRankingJob(Step clearExistingRankingStep, Step calculateRankingStep) {
        return new JobBuilder("monthlyRankingJob", jobRepository)
                .start(clearExistingRankingStep)
                .next(calculateRankingStep)
                .build();
    }

    @Bean
    public Step clearExistingRankingStep(ClearExistingRankingTasklet tasklet) {
        return new StepBuilder("clearExistingRankingStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step calculateRankingStep(
            MyBatisCursorItemReader<MemberRankingDto> rankingReader,
            ItemProcessor<MemberRankingDto, MemberRankingDto> rankingProcessor,
            MyBatisBatchItemWriter<MemberRankingDto> rankingWriter) {
        
        return new StepBuilder("calculateRankingStep", jobRepository)
                .<MemberRankingDto, MemberRankingDto>chunk(100, transactionManager)
                .reader(rankingReader)
                .processor(rankingProcessor)
                .writer(rankingWriter)
                .build();
    }

    @Bean
    public ItemProcessor<MemberRankingDto, MemberRankingDto> rankingProcessor() {
        return item -> {
            if (item.getTotalAmount() <= 0) {
                return null;
            }
            return item;
        };
    }

    @Bean
    @StepScope
    public MyBatisCursorItemReader<MemberRankingDto> rankingReader(
            @Value("#{jobParameters['targetYearMonth']}") String targetYearMonth) {
        
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("targetYearMonth", targetYearMonth);

        return new MyBatisCursorItemReaderBuilder<MemberRankingDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.doumi.donation.ranking.model.dao.RankingDao.selectMonthlyCalculatedRanking")
                .parameterValues(parameterValues)
                .build();
    }

    @Bean
    @StepScope
    public MyBatisBatchItemWriter<MemberRankingDto> rankingWriter() {
        return new MyBatisBatchItemWriterBuilder<MemberRankingDto>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.doumi.donation.ranking.model.dao.RankingDao.insertRankingBatch")
                .assertUpdates(false)
                .build();
    }
}
