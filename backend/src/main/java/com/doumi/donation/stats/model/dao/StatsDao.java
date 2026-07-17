package com.doumi.donation.stats.model.dao;

import com.doumi.donation.stats.model.dto.LabelCountDto;
import com.doumi.donation.stats.model.dto.MonthlyDonationDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StatsDao {
    long getTotalDonationAmount();
    long getActiveCampaignCount();
    long getTotalMemberCount();
    long getMonthlyDonorCount();
    long getCompletedCampaignCount();

    long getIndividualMemberCount();
    long getOrganizationMemberCount();
    long getPendingOrganizationCount();

    List<MonthlyDonationDto> getMonthlyDonations();
    List<LabelCountDto> getCategoryCounts();
    List<LabelCountDto> getRegionCounts();
}
