package com.doumi.donation.stats.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class StatsResponse {
    private long totalDonationAmount; // 총 누적 기부금
    private long activeCampaigns;     // 진행 중인 캠페인 수
    private long totalMembers;        // 가입 회원 수
    private long monthlyDonors;       // 이번 달 기부자 수
    private long completedCampaigns;  // 완료된 캠페인 수

    private long individualMembers;     // 개인 회원 수
    private long organizationMembers;   // 단체 회원 수
    private long pendingOrganizations;  // 승인 대기 단체 수

    private List<MonthlyDonationDto> monthlyDonations; // 최근 6개월 월별 기부 합계
    private List<LabelCountDto> categoryCounts;        // 카테고리별 캠페인 수
    private List<LabelCountDto> regionCounts;          // 지역별 캠페인 수
}
