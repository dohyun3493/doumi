package com.doumi.donation.campaigns.model.dto;

import lombok.Data;

/** 캠페인 삭제 요청/거부 사유 */
@Data
public class DeleteRequest {
    private String reason;
}
