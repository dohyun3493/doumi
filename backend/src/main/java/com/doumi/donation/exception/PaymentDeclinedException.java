package com.doumi.donation.exception;

/**
 * 토스가 <b>명시적으로 거절</b>한 경우 (카드 한도 초과, 잘못된 금액 등 4xx 응답).
 *
 * <p>결과가 확정된 실패이므로 결제 건을 즉시 FAILED로 종결해도 안전하다.
 * 반면 타임아웃처럼 결과를 알 수 없는 경우는 {@link PaymentPendingException}으로 구분한다.
 *
 * <p>{@code IllegalArgumentException}을 상속해 기존 핸들러의 400 응답 규칙을 그대로 따른다.
 */
public class PaymentDeclinedException extends IllegalArgumentException {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}
