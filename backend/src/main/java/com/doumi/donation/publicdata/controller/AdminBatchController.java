package com.doumi.donation.publicdata.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminBatchController {

    private final JobLauncher jobLauncher;
    private final Job publicDataImportJob;
    private final JobExplorer jobExplorer;

    @Autowired
    public AdminBatchController(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
                                Job publicDataImportJob,
                                JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.publicDataImportJob = publicDataImportJob;
        this.jobExplorer = jobExplorer;
    }

    @PostMapping("/public-data")
    public ResponseEntity<String> runPublicDataImportJob() {
        try {
            // 동시 실행 방지: 이미 실행 중인 Job이 있으면 거부
            Set<JobExecution> runningExecutions =
                    jobExplorer.findRunningJobExecutions("publicDataImportJob");
            if (!runningExecutions.isEmpty()) {
                log.warn("배치 중복 실행 시도 차단 - 이미 실행 중인 Job이 {}건 있습니다.", runningExecutions.size());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("이미 실행 중인 배치 작업이 있습니다. 완료 후 다시 시도해주세요.");
            }

            log.info("관리자 요청: 공공데이터 마이그레이션 배치 시작");
            
            // 매번 새로운 JobExecution으로 실행할 수 있도록 고유 파라미터 부여
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            jobLauncher.run(publicDataImportJob, jobParameters);
            
            log.info("배치 실행 트리거 성공 - 비동기 구동");
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("공공데이터 수집 배치 작업이 백그라운드에서 시작되었습니다.");
        } catch (Exception e) {
            log.error("배치 실행 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("배치 실행 실패: " + e.getMessage());
        }
    }
}
