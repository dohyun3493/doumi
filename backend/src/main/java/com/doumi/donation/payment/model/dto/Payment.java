package com.doumi.donation.payment.model.dto;

import lombok.Data;

/** 토스 결제 승인 원장 — 환불(결제취소)과 분쟁 대응의 근거 */
@Data
public class Payment {
    private long paymentId;
    private long memberId;
    private String paymentKey;
    private String orderId;
    private long amount;
    private String status;      // CHARGED | CANCELED
    private String createdAt;
    private String canceledAt;
}
