package com.doumi.donation.payment.model.dto;

import lombok.Data;

/** 결제 취소(환불) 요청 — 사유는 선택값 */
@Data
public class CancelRequest {
    private String reason;
}
