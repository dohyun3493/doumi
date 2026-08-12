package com.doumi.donation.payment.model.dao;

import com.doumi.donation.payment.model.dto.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 결제 원장 접근.
 *
 * <p>상태 전이는 모두 <b>조건부 UPDATE</b>({@code WHERE status = 이전상태})로 되어 있다.
 * 갱신 건수가 0이면 다른 요청(또는 스케줄러)이 이미 처리한 것이므로, 같은 작업이
 * 여러 번 실행돼도 한 번만 반영된다(멱등).
 */
@Mapper
public interface PaymentDao {
    /** 토스 승인 <b>전에</b> 시도 사실을 먼저 남긴다. 커밋 후 paymentId가 채워진다. */
    void insertPending(Payment payment);
    List<Payment> findByMemberId(@Param("memberId") long memberId);
    boolean existsByPaymentKey(@Param("paymentKey") String paymentKey);
    Payment findById(@Param("paymentId") long paymentId);

    /** PENDING → CHARGED (승인 확정) */
    int markCharged(@Param("paymentId") long paymentId);
    /** PENDING → FAILED (승인 거절 확정) */
    int markFailed(@Param("paymentId") long paymentId);
    /** CHARGED → CANCELING (취소 착수. 동시 취소 요청을 막는 관문) */
    int markCanceling(@Param("paymentId") long paymentId);
    /** CANCELING → CANCELED (취소 확정) */
    int markCanceled(@Param("paymentId") long paymentId);
    /** CANCELING → CHARGED (취소 실패 시 원복) */
    int restoreCharged(@Param("paymentId") long paymentId);

    /** 결과를 모른 채 방치된 건(PENDING·CANCELING) 조회 — 스케줄러의 복구 대상 */
    List<Payment> findStuck(@Param("staleMinutes") int staleMinutes);
}
