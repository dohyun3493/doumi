package com.doumi.donation.campaigns.model.dto;

import lombok.Data;

/**
 * 단체 탈퇴 시 캠페인 기부자에게 환불할 정보.
 * (기부자별·캠페인별 기부 합계 — 환불 + 알림에 사용)
 */
@Data
public class DonorRefundDto {
    private long memberId;        // 환불 대상 기부자
    private long campaignId;
    private String campaignTitle; // 알림 문구용
    private long amount;          // 해당 캠페인에 기부한 총액
}
