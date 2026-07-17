package com.doumi.donation.ranking.config;

import com.doumi.donation.ranking.model.dao.RankingDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClearExistingRankingTasklet implements Tasklet {

    private final RankingDao rankingDao;

    @Autowired
    public ClearExistingRankingTasklet(RankingDao rankingDao) {
        this.rankingDao = rankingDao;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String targetYearMonth = (String) chunkContext.getStepContext()
                .getJobParameters()
                .get("targetYearMonth");

        log.info("스케줄러 작업 대상월 데이터 초기화 시작: {}", targetYearMonth);
        int deletedRows = rankingDao.deleteRankingByMonth(targetYearMonth);
        log.info("초기화 완료 - 대상월: {}, 삭제 건수: {}건", targetYearMonth, deletedRows);

        return RepeatStatus.FINISHED;
    }
}
