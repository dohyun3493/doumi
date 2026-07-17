package com.doumi.donation.report.model.dto;

import lombok.Data;

/** 사용 보고에 첨부된 사진 (영수증 / 후기) */
@Data
public class ReportImage {
    private long imageId;
    private long reportId;
    private String imageUrl;    // 이미지 경로 (/uploads/...)
    private String imageType;   // RECEIPT(영수증) | REVIEW(후기)
}
