package com.doumi.donation.payment.service;

import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.PaymentDeclinedException;
import com.doumi.donation.payment.client.TossPaymentClient;
import com.doumi.donation.payment.model.dao.PaymentDao;
import com.doumi.donation.payment.model.dto.ConfirmRequest;
import com.doumi.donation.payment.model.dto.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 결제 흐름을 조율한다. <b>이 클래스에는 트랜잭션이 걸려 있지 않다.</b>
 *
 * <p>토스 승인/취소는 외부 HTTP 호출이라 DB 트랜잭션 안에서 부르면 두 가지 문제가 있다.
 * 첫째, 응답을 기다리는 동안 DB 커넥션이 반납되지 않아 커넥션 풀이 고갈되고 결제와 무관한
 * API까지 함께 죽는다. 둘째, HTTP 호출은 롤백되지 않으므로 트랜잭션에 묶어봐야 원자성이
 * 보장되지 않는다 — 오히려 보장되는 것처럼 착각하게 만든다.
 *
 * <p>그래서 외부 호출은 여기(트랜잭션 밖)에서 하고, DB 작업은 {@link PaymentTxService}의
 * 짧은 트랜잭션에 맡긴다. 외부 상태는 롤백할 수 없으므로 되돌리는 대신 <b>앞으로 민다</b>:
 * 호출 전에 PENDING을 커밋해 흔적을 남기고, 결과를 모른 채 중단되면 그 흔적을 근거로
 * 스케줄러가 토스에 재조회해 확정한다.
 */
@Service
public class PaymentServiceImp implements PaymentService {

    private final PaymentTxService paymentTxService;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentDao paymentDao;

    public PaymentServiceImp(PaymentTxService paymentTxService,
                             TossPaymentClient tossPaymentClient,
                             PaymentDao paymentDao) {
        this.paymentTxService = paymentTxService;
        this.tossPaymentClient = tossPaymentClient;
        this.paymentDao = paymentDao;
    }

    /**
     * 결제 승인 + 포인트 충전.
     *
     * <p>중간에 죽으면 결제 건은 PENDING으로 남고, 스케줄러가 토스 조회로 확정한다.
     * 그래서 사용자에게 "실패"가 아니라 "처리 중"으로 안내해야 한다
     * ({@code PaymentPendingException} → 202).
     */
    @Override
    public void confirm(long memberId, ConfirmRequest req) {
        // 0. 멱등성 사전 체크: 이미 시도된 결제면 토스 재호출 없이 즉시 409
        //    (더블클릭 등 흔한 경우를 싸게 차단. 동시 요청 경합은 1번의 UNIQUE 제약이 막는다)
        if (paymentDao.existsByPaymentKey(req.getPaymentKey())) {
            throw new DuplicateResourceException("이미 처리된 결제입니다.");
        }

        // 1. 승인 시도 사실을 먼저 커밋 (짧은 DB 트랜잭션)
        long paymentId = paymentTxService.createPending(memberId, req);

        // 2. 토스페이먼츠 결제 승인 — 트랜잭션 밖에서 호출
        try {
            tossPaymentClient.approve(req);
        } catch (PaymentDeclinedException e) {
            // 토스가 명시적으로 거절 → 결과가 확정됐으므로 즉시 종결
            paymentTxService.markFailed(paymentId);
            throw e;
        }
        // 결과 불명(PaymentPendingException)은 여기서 잡지 않는다.
        // PENDING으로 남겨둬야 스케줄러가 재조회해 복구할 수 있다.

        // ★ 이 지점에서 프로세스가 죽어도 PENDING이 남아 스케줄러가 복구한다

        // 3. 승인 확정 + 포인트 충전 (짧은 DB 트랜잭션)
        paymentTxService.completeCharge(paymentId, memberId, req.getAmount());
    }

    /**
     * 충전 취소(환불).
     *
     * <p>취소 착수(상태 전이 + 포인트 회수) → 토스 취소 → 확정 순서로 처리한다.
     * 포인트 회수를 토스 호출보다 먼저 두어, 이미 써버려 잔액이 부족하면 되돌릴 수 없는
     * 환불을 실행하기 전에 중단한다.
     */
    @Override
    public void cancel(long memberId, long paymentId, String reason) {
        // 1. 취소 착수 (DB 트랜잭션): 검증 → CHARGED→CANCELING → 포인트 회수
        Payment payment = paymentTxService.beginCancel(memberId, paymentId);

        // 2. 토스 결제 취소 — 트랜잭션 밖에서 호출
        try {
            tossPaymentClient.cancel(payment.getPaymentKey(), reason);
        } catch (PaymentDeclinedException e) {
            // 확정 실패 → 1번 트랜잭션은 이미 커밋되어 자동 롤백되지 않으므로 보상으로 원복
            paymentTxService.revertCancel(memberId, paymentId, payment.getAmount());
            throw e;
        }
        // 결과 불명은 CANCELING으로 남겨 스케줄러가 확정하도록 둔다

        // ★ 이 지점에서 죽어도 CANCELING이 남아 스케줄러가 복구한다

        // 3. 취소 확정 (CANCELING → CANCELED)
        paymentTxService.completeCancel(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByMemberId(long memberId) {
        return paymentDao.findByMemberId(memberId);
    }
}
