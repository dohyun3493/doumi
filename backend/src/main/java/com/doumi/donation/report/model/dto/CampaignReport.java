package com.doumi.donation.report.model.dto;

import lombok.Data;

import java.util.List;

/** 기부금 사용 보고 (캠페인당 1개) */
@Data
public class CampaignReport {
    private long reportId;
    private long campaignId;
    private String content;
    private String createdAt;
    private List<ReportExpense> expenses;
    private List<ReportImage> images;   // 첨부 사진 (영수증 / 후기)
    private long totalSpent;    // 지출 합계 (조회 시 계산)
}
