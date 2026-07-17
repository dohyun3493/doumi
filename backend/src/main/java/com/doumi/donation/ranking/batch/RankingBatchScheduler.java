package com.doumi.donation.ranking.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RankingBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job monthlyRankingJob;

    @Autowired
    public RankingBatchScheduler(JobLauncher jobLauncher, Job monthlyRankingJob) {
        this.jobLauncher = jobLauncher;
        this.monthlyRankingJob = monthlyRankingJob;
    }

    @Scheduled(cron = "${ranking.batch.cron:0 0 2 1 * *}")
    public void runMonthlyRankingJob() {
        try {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String targetMonthStr = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            log.info("정기 스케줄러 가동: {} 월간 기부 랭킹 집계 시작", targetMonthStr);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetYearMonth", targetMonthStr)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(monthlyRankingJob, jobParameters);
            log.info("정기 스케줄러 완료: {} 월간 기부 랭킹 집계 실행 완료", targetMonthStr);
        } catch (Exception e) {
            log.error("정기 스케줄러 가동 중 예외 발생", e);
        }
    }
}
