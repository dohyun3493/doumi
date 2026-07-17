package com.doumi.donation.stats.model.dto;

import lombok.Data;

/** 월별 기부 합계 (대시보드 최근 6개월) */
@Data
public class MonthlyDonationDto {
    private String month;   // 예: 2026-06
    private long amount;    // 해당 월 기부 합계
}
