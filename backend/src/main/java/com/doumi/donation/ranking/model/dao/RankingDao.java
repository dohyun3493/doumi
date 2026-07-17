package com.doumi.donation.ranking.model.dao;

import com.doumi.donation.ranking.model.dto.RankingResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RankingDao {
    // [배치-Tasklet] 대상월 기존 랭킹 삭제
    int deleteRankingByMonth(@Param("targetYearMonth") String targetYearMonth);
    // [조회 API] 특정 월 상위 N명 조회
    List<RankingResponse> selectTopRankings(@Param("yearMonth") String yearMonth, @Param("limit") int limit);
}
