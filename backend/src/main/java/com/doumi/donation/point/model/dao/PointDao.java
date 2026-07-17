package com.doumi.donation.point.model.dao;

import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.point.model.dto.PointHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointDao {
    void insertHistory(PointHistory history);
    void insertDonation(@Param("memberId") long memberId,
                        @Param("campaignId") long campaignId,
                        @Param("amount") long amount);
    /** 기부 대상 캠페인을 행 잠금(FOR UPDATE)으로 조회 — 동시 기부 시 금액/상태 정합성 보장 */
    Campaign findCampaignForDonation(@Param("campaignId") long campaignId);
    void updateCampaignAmount(@Param("campaignId") long campaignId, @Param("amount") long amount);
    List<PointHistory> findHistoryByMemberId(@Param("memberId") long memberId);
}
