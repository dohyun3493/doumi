package com.doumi.donation.campaigns.model.dto;

import lombok.Data;

@Data
public class Campaign {
    private long campaignId;
    private String apiRegNo;
    private String orgName;
    private String title;
    private String description;
    private String purpose;
    private Long goalAmount;
    private Long currentAmount;
    private String region;
    private String category;
    private String startDate;
    private String endDate;
    private String status;
    private String homepageUrl;
    private String zipCode;
    private String address;
    private String telNo;
    private String appliedAt;
    private Long ownerId;   // 등록 단체 member_id (공공데이터 캠페인은 null)
    private String thumbnailUrl;   // 대표 이미지 경로 (/uploads/...)

    private boolean deleteRequested;       // 단체의 삭제 요청 여부
    private String deleteRequestReason;    // 삭제 요청 사유
}
