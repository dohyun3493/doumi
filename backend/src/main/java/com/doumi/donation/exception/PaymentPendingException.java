package com.doumi.donation.exception;

/**
 * 토스 호출 결과를 <b>알 수 없는</b> 경우 (타임아웃, 커넥션 오류, 5xx).
 *
 * <p>승인이 됐을 수도, 안 됐을 수도 있다. 이때 "실패했습니다"라고 단정하면
 * 실제로는 승인된 결제를 사용자가 다시 시도하게 만든다. 그래서 실패가 아니라
 * <b>처리 중</b>으로 안내하고(202), 결제 건은 PENDING/CANCELING으로 남겨
 * 스케줄러가 토스에 재조회해 확정하도록 한다.
 */
public class PaymentPendingException extends RuntimeException {
    public PaymentPendingException(String message) {
        super(message);
    }
}
