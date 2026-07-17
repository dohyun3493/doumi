package com.doumi.donation.member.model.dto;

import lombok.Data;

@Data
public class MyDonationDto {
    private long donationId;
    private String campaignTitle;
    private long campaignId;
    private long amount;
    private String donatedAt;
}
