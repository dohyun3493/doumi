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
    // PENDING(승인 시도, 결과 미확인) | CHARGED(승인+충전 완료) | CANCELING(취소 착수, 결과 미확인)
    // | CANCELED(취소 완료) | FAILED(승인 거절)
    private String status;
    private String createdAt;
    private String canceledAt;
}
