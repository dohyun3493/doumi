package com.doumi.donation.campaigns.model.dto;

import lombok.Data;

@Data
public class DonationDto {
    private long donationId;
    private String maskedName; // 김*현 형태로 마스킹
    private long amount;
    private String donatedAt;
}
