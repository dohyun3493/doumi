package com.doumi.donation.payment.service;

import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.payment.model.dao.PaymentDao;
import com.doumi.donation.payment.model.dto.ConfirmRequest;
import com.doumi.donation.payment.model.dto.Payment;
import com.doumi.donation.point.model.dto.ChargeRequest;
import com.doumi.donation.point.service.PointService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 처리 중 <b>DB 작업만</b> 담은 트랜잭션 단위들.
 *
 * <p>{@link PaymentServiceImp}와 별도의 빈으로 분리한 이유는 스프링 AOP가 프록시 기반이라
 * 같은 클래스 안에서의 self-invocation은 가로채지 못해 {@code @Transactional}이 무시되기 때문이다.
 * 외부 API 호출을 트랜잭션 밖으로 빼려면 이 분리가 필수다.
 *
 * <p>모든 메서드가 DB 작업만 하므로 트랜잭션이 짧게 유지되고, 커넥션이 외부 I/O 대기에
 * 묶이지 않는다. 또한 상태 전이가 조건부 UPDATE라서 요청 처리와 복구 스케줄러가
 * 동시에 같은 건을 건드려도 한 번만 반영된다.
 */
@Service
public class PaymentTxService {

    private final PointService pointService;
    private final PaymentDao paymentDao;

    public PaymentTxService(PointService pointService, PaymentDao paymentDao) {
        this.pointService = pointService;
        this.paymentDao = paymentDao;
    }

    /**
     * 토스 승인 <b>전에</b> 시도 사실을 PENDING으로 남기고 커밋한다.
     *
     * <p>이 커밋이 이 설계의 핵심이다. 흔적을 먼저 남겨야 이후 어느 지점에서 죽더라도
     * "이 결제를 시도했다"는 사실이 DB에 남고, 스케줄러가 토스에 재조회해 복구할 수 있다.
     * 기존처럼 승인 뒤에 기록하면 그 사이 장애 시 승인만 남고 기록이 증발한다.
     *
     * <p>payment_key UNIQUE 제약이 중복 결제를 여기서 차단한다. 사전 조회는 동시 요청에
     * 뚫리는 check-then-act지만, 이 INSERT는 DB가 보장하므로 최종 방어선이 된다.
     * 게다가 외부 호출보다 <b>앞</b>에 있어 불필요한 토스 호출까지 막는다.
     *
     * @return 생성된 결제 건의 id
     */
    @Transactional
    public long createPending(long memberId, ConfirmRequest req) {
        try {
            Payment payment = new Payment();
            payment.setMemberId(memberId);
            payment.setPaymentKey(req.getPaymentKey());
            payment.setOrderId(req.getOrderId());
            payment.setAmount(req.getAmount());
            paymentDao.insertPending(payment);
            return payment.getPaymentId();
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 결제가 들어온 경합(UNIQUE payment_key 위반) → 500이 아닌 409로 변환
            throw new DuplicateResourceException("이미 처리된 결제입니다.");
        }
    }

    /**
     * 승인 확정: PENDING → CHARGED 전이와 포인트 충전을 한 트랜잭션으로 묶는다.
     *
     * <p>둘을 같은 트랜잭션에 두어야 "CHARGED인데 포인트는 없는" 상태가 생기지 않고,
     * {@code status = CHARGED}가 곧 "충전까지 끝났다"는 뜻이 된다.
     *
     * <p>전이가 0건이면 이미 다른 주체(스케줄러 또는 원 요청)가 확정한 것이므로
     * 포인트를 지급하지 않고 조용히 끝낸다 — 중복 충전 방지.
     */
    @Transactional
    public void completeCharge(long paymentId, long memberId, long amount) {
        if (paymentDao.markCharged(paymentId) == 0) {
            return;
        }
        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setAmount(amount);
        pointService.charge(memberId, chargeRequest);
    }

    /** 승인 거절 확정: PENDING → FAILED. 결과가 확실할 때만 호출해야 한다. */
    @Transactional
    public void markFailed(long paymentId) {
        paymentDao.markFailed(paymentId);
    }

    /**
     * 취소 착수. 권한 검증 → CHARGED → CANCELING 전이 → 포인트 회수를 한 트랜잭션으로 처리하고,
     * 실제 토스 취소 호출은 이 트랜잭션이 끝난 뒤 밖에서 이뤄진다.
     *
     * <p>상태 전이를 먼저 시도해 0건이면 중단한다. 조건부 UPDATE가 행 락을 잡아 동시 취소 요청을
     * 직렬화하므로, 락 없는 SELECT로 검사할 때 생기던 경합(두 요청이 모두 통과해 포인트가
     * 두 번 회수되는 문제)이 사라진다.
     *
     * <p>포인트 회수를 토스 호출보다 먼저 두는 것은 의도적이다. 이미 기부 등으로 써버려
     * 잔액이 부족하면 여기서 예외가 나고 상태 전이까지 함께 롤백되므로, 되돌릴 수 없는
     * 환불을 실행하기 전에 정합성을 확인할 수 있다.
     */
    @Transactional
    public Payment beginCancel(long memberId, long paymentId) {
        Payment payment = paymentDao.findById(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("존재하지 않는 결제 내역입니다.");
        }
        if (payment.getMemberId() != memberId) {
            throw new UnauthorizedException("본인의 결제만 취소할 수 있습니다.");
        }
        if (paymentDao.markCanceling(paymentId) == 0) {
            throw new DuplicateResourceException("이미 취소되었거나 취소 처리 중인 결제입니다.");
        }

        pointService.refund(memberId, payment.getAmount());
        return payment;
    }

    /** 취소 확정: CANCELING → CANCELED. 포인트는 착수 단계에서 이미 회수됐다. */
    @Transactional
    public void completeCancel(long paymentId) {
        paymentDao.markCanceled(paymentId);
    }

    /**
     * 보상 트랜잭션. 토스 취소가 <b>확정 실패</b>했을 때 {@link #beginCancel}이 이미 커밋한
     * 변경(상태 전이 + 포인트 회수)을 되돌린다.
     *
     * <p>호출이 트랜잭션 밖이라 예외가 나도 앞 트랜잭션이 자동 롤백되지 않으므로
     * 이렇게 명시적으로 원복해야 한다. 상태가 CANCELING일 때만 원복되므로,
     * 이미 확정된 건을 잘못 되돌리지 않는다.
     */
    @Transactional
    public void revertCancel(long memberId, long paymentId, long amount) {
        if (paymentDao.restoreCharged(paymentId) == 0) {
            return;
        }
        pointService.revertRefund(memberId, amount);
    }
}
