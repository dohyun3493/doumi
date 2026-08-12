package com.doumi.donation.payment.batch;

import com.doumi.donation.payment.client.TossPaymentClient;
import com.doumi.donation.payment.model.dao.PaymentDao;
import com.doumi.donation.payment.model.dto.Payment;
import com.doumi.donation.payment.model.dto.TossPaymentStatus;
import com.doumi.donation.payment.service.PaymentTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 결과를 모른 채 방치된 결제 건을 토스에 재조회해 확정한다(대사, reconciliation).
 *
 * <p>외부 호출은 롤백할 수 없다. 그래서 승인 직후 장애가 나면 되돌리는 대신
 * <b>앞으로 밀어야</b> 한다. 이 스케줄러가 그 역할을 맡는다.
 * <ul>
 *   <li>PENDING — 승인됐으면 CHARGED + 포인트 지급, 아니면 FAILED로 종결</li>
 *   <li>CANCELING — 취소됐으면 CANCELED 확정, 아니면 포인트 회수를 원복</li>
 * </ul>
 *
 * <p>이 스케줄러가 없으면 "돈은 빠졌는데 포인트는 없는" 건이 수동 대사로만 해결된다.
 *
 * <p>서버를 여러 대 띄워 중복 실행되더라도 안전하다. 모든 상태 전이가 조건부 UPDATE라
 * 행 락으로 직렬화되고 한쪽만 성공하기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconcileScheduler {

    private final PaymentDao paymentDao;
    private final PaymentTxService paymentTxService;
    private final TossPaymentClient tossPaymentClient;

    /** 정상 처리 중인 요청까지 건드리지 않도록 두는 유예 시간 */
    @Value("${payment.reconcile.stale-minutes:5}")
    private int staleMinutes;

    @Scheduled(fixedDelayString = "${payment.reconcile.interval-ms:60000}")
    public void reconcile() {
        List<Payment> stuck = paymentDao.findStuck(staleMinutes);
        if (stuck.isEmpty()) {
            return;
        }
        log.info("결과 미확정 결제 {}건 대사를 시작합니다.", stuck.size());

        for (Payment payment : stuck) {
            try {
                reconcileOne(payment);
            } catch (Exception e) {
                // 한 건의 실패가 나머지 복구를 막지 않도록 격리 (다음 주기에 다시 시도)
                log.error("결제 대사 실패: paymentId={}, status={}",
                        payment.getPaymentId(), payment.getStatus(), e);
            }
        }
    }

    private void reconcileOne(Payment payment) {
        // 진실의 원천은 토스다. 우리 DB는 "모른다"고만 알고 있다.
        TossPaymentStatus actual = tossPaymentClient.findStatus(payment.getPaymentKey());

        if ("PENDING".equals(payment.getStatus())) {
            if (actual.isApproved()) {
                paymentTxService.completeCharge(
                        payment.getPaymentId(), payment.getMemberId(), payment.getAmount());
                log.info("승인 확인 → 충전 완료 처리: paymentId={}", payment.getPaymentId());
            } else {
                paymentTxService.markFailed(payment.getPaymentId());
                log.info("미승인 확인 → 실패 처리: paymentId={}, tossStatus={}",
                        payment.getPaymentId(), actual.status());
            }
        } else { // CANCELING
            if (actual.isCanceled()) {
                paymentTxService.completeCancel(payment.getPaymentId());
                log.info("취소 확인 → 취소 확정: paymentId={}", payment.getPaymentId());
            } else {
                paymentTxService.revertCancel(
                        payment.getMemberId(), payment.getPaymentId(), payment.getAmount());
                log.info("미취소 확인 → 취소 원복: paymentId={}, tossStatus={}",
                        payment.getPaymentId(), actual.status());
            }
        }
    }
}
