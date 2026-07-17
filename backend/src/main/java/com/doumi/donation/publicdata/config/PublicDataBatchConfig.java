package com.doumi.donation.publicdata.config;

import com.doumi.donation.chatbot.service.CampaignIndexService;
import com.doumi.donation.publicdata.batch.PublicDataApiItemReader;
import com.doumi.donation.publicdata.batch.PublicDataProcessor;
import com.doumi.donation.publicdata.batch.PublicDataWriter;
import com.doumi.donation.publicdata.model.dto.DonationProgramDto;
import com.doumi.donation.publicdata.service.PublicDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientException;

@Slf4j
@Configuration
public class PublicDataBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PublicDataService publicDataService;
    private final PublicDataProcessor publicDataProcessor;
    private final PublicDataWriter publicDataWriter;

    public PublicDataBatchConfig(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 PublicDataService publicDataService,
                                 PublicDataProcessor publicDataProcessor,
                                 PublicDataWriter publicDataWriter) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.publicDataService = publicDataService;
        this.publicDataProcessor = publicDataProcessor;
        this.publicDataWriter = publicDataWriter;
    }

    @Bean
    public Job publicDataImportJob(Step campaignWipeStep, Step publicDataImportStep,
                                   CampaignIndexService campaignIndexService) {
        return new JobBuilder("publicDataImportJob", jobRepository)
                // 전체 리로드: 먼저 기존 데이터를 비우고(wipe) → 그 다음 API 수집(import)
                .start(campaignWipeStep)
                .next(publicDataImportStep)
                // 수집 완료 후 챗봇용 벡터 저장소에 캠페인 재색인 (신규 캠페인 자동 반영)
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
                            return;
                        }
                        try {
                            int indexed = campaignIndexService.reindexAll();
                            log.info("공공데이터 수집 후 캠페인 벡터 재색인 완료: {}건", indexed);
                        } catch (Exception e) {
                            // 색인 실패가 배치 결과를 바꾸지는 않음 (관리자 reindex API로 재시도 가능)
                            log.error("캠페인 벡터 재색인 실패", e);
                        }
                    }
                })
                .build();
    }

    /**
     * 전체 리로드 1단계: 기존 기부내역 + 캠페인을 모두 삭제한다.
     * ⚠️ 되돌릴 수 없음. import 단계가 이어서 API 데이터를 다시 채운다.
     */
    @Bean
    public Step campaignWipeStep() {
        return new StepBuilder("campaignWipeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    publicDataService.clearAllForReload();
                    log.info("전체 리로드: 기존 기부내역 + 캠페인을 모두 삭제했습니다.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step publicDataImportStep() {
        return new StepBuilder("publicDataImportStep", jobRepository)
                .<DonationProgramDto, DonationProgramDto>chunk(100, transactionManager)
                .reader(publicDataApiItemReader())
                .processor(publicDataProcessor)
                .writer(publicDataWriter)
                .faultTolerant()
                .retryLimit(3)                              // 일시적 오류 시 최대 3회 재시도
                .retry(RestClientException.class)            // REST API 호출 오류
                .retry(java.net.SocketTimeoutException.class) // 소켓 타임아웃
                .skipLimit(10)                               // 최대 10건까지 건너뛰기
                .skip(Exception.class)                       // 재시도 후에도 실패한 건은 스킵
                .noSkip(java.lang.NullPointerException.class) // NPE는 코드 버그이므로 스킵 금지
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<DonationProgramDto> publicDataApiItemReader() {
        return new PublicDataApiItemReader(publicDataService);
    }

    /**
     * 비동기 배치를 위한 JobLauncher 빈 정의
     */
    @Bean(name = "asyncJobLauncher")
    public JobLauncher asyncJobLauncher() throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}

