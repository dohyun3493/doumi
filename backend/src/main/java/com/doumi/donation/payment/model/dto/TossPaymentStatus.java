package com.doumi.donation.payment.model.dto;

/**
 * 토스 결제 조회 API가 알려주는 <b>실제</b> 결제 상태.
 *
 * <p>우리 DB가 PENDING/CANCELING으로 남아 결과를 모를 때, 진실의 원천은 토스다.
 * 스케줄러가 이 값을 근거로 결제 건을 어느 쪽으로 확정할지 결정한다.
 */
public record TossPaymentStatus(String status) {

    /** 조회 결과 해당 결제가 아예 없음 → 승인이 도달하지 못한 것 */
    public static TossPaymentStatus notFound() {
        return new TossPaymentStatus(null);
    }

    /** 승인 완료 */
    public boolean isApproved() {
        return "DONE".equals(status);
    }

    /** 취소 완료 (부분 취소 포함) */
    public boolean isCanceled() {
        return "CANCELED".equals(status) || "PARTIAL_CANCELED".equals(status);
    }
}
