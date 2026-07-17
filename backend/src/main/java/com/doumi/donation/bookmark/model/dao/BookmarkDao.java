package com.doumi.donation.bookmark.model.dao;

import com.doumi.donation.campaigns.model.dto.Campaign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookmarkDao {
    /** INSERT IGNORE — 이미 찜한 경우 무시 (UNIQUE 제약 활용) */
    int insertBookmark(@Param("memberId") long memberId, @Param("campaignId") long campaignId);
    int deleteBookmark(@Param("memberId") long memberId, @Param("campaignId") long campaignId);
    List<Campaign> findBookmarkedCampaigns(@Param("memberId") long memberId);
    boolean existsCampaign(@Param("campaignId") long campaignId);
}
