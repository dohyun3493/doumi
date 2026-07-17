package com.doumi.donation.payment.service;

import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.payment.model.dao.PaymentDao;
import com.doumi.donation.payment.model.dto.ConfirmRequest;
import com.doumi.donation.payment.model.dto.Payment;
import com.doumi.donation.point.model.dto.ChargeRequest;
import com.doumi.donation.point.service.PointService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class PaymentServiceImp implements PaymentService {

    private final PointService pointService;
    private final PaymentDao paymentDao;
    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @Value("${toss.secret-key}")
    private String secretKey;

    @Value("${toss.confirm-url}")
    private String confirmUrl;

    // 결제 취소 베이스 URL (실제 호출: {cancelUrl}/{paymentKey}/cancel)
    @Value("${toss.cancel-url}")
    private String cancelUrl;

    public PaymentServiceImp(PointService pointService, PaymentDao paymentDao) {
        this.pointService = pointService;
        this.paymentDao = paymentDao;
    }

    // 원장 기록(insertPayment)과 포인트 충전(charge)을 하나의 트랜잭션으로 묶어
    // 둘 중 하나라도 실패하면 함께 롤백 → "결제는 됐는데 포인트 미지급" 같은 불일치 방지.
    @Override
    @Transactional
    public void confirm(long memberId, ConfirmRequest req) {
        // 0. 멱등성 사전 체크: 이미 승인 처리된 결제면 토스 재호출 없이 즉시 409
        //    (사용자 더블클릭 등으로 같은 paymentKey가 재요청되는 흔한 경우를 깔끔하게 차단)
        if (paymentDao.existsByPaymentKey(req.getPaymentKey())) {
            throw new DuplicateResourceException("이미 처리된 결제입니다.");
        }

        // 1. 토스페이먼츠 결제 승인
        approvePayment(req);

        // 2. 결제 원장 기록 (paymentKey가 있어야 이후 환불(결제취소) 가능)
        try {
            Payment payment = new Payment();
            payment.setMemberId(memberId);
            payment.setPaymentKey(req.getPaymentKey());
            payment.setOrderId(req.getOrderId());
            payment.setAmount(req.getAmount());
            paymentDao.insertPayment(payment);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 결제가 들어온 경합(UNIQUE payment_key 위반) → 500이 아닌 409로 변환
            throw new DuplicateResourceException("이미 처리된 결제입니다.");
        }

        // 3. 승인 성공 → 포인트 충전 (같은 트랜잭션에 합류해 원장과 원자적으로 처리됨)
        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setAmount(req.getAmount());
        pointService.charge(memberId, chargeRequest);
    }

    // 충전 취소(환불): 포인트 회수 → 토스 결제취소 → 원장 갱신을 하나의 트랜잭션으로 묶음.
    // 포인트 회수를 먼저 시도해(잔액 부족이면 즉시 중단) 환불 전에 정합성을 확인하고,
    // 토스 취소가 실패하면 트랜잭션이 롤백되어 회수했던 포인트도 원복된다.
    @Override
    @Transactional
    public void cancel(long memberId, long paymentId, String reason) {
        Payment payment = paymentDao.findById(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("존재하지 않는 결제 내역입니다.");
        }
        if (payment.getMemberId() != memberId) {
            throw new UnauthorizedException("본인의 결제만 취소할 수 있습니다.");
        }
        if ("CANCELED".equals(payment.getStatus())) {
            throw new DuplicateResourceException("이미 취소된 결제입니다.");
        }

        // 1. 충전했던 포인트 회수 (이미 써버려 잔액 부족이면 여기서 예외 → 환불 진행 안 함)
        pointService.refund(memberId, payment.getAmount());

        // 2. 토스 결제 취소 (실패 시 예외 → 1번 포인트 회수까지 함께 롤백)
        cancelPayment(payment.getPaymentKey(), reason);

        // 3. 원장 상태 갱신 (CHARGED → CANCELED)
        paymentDao.cancelPayment(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByMemberId(long memberId) {
        return paymentDao.findByMemberId(memberId);
    }

    // 토스 결제 취소 API 호출: POST {cancelUrl}/{paymentKey}/cancel  body: { cancelReason }
    private void cancelPayment(String paymentKey, String reason) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> body = Map.of(
                "cancelReason", (reason == null || reason.isBlank()) ? "사용자 요청" : reason
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = cancelUrl + "/" + paymentKey + "/cancel";

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("결제 취소에 실패했습니다.");
            }
        } catch (RestClientResponseException e) {
            // 토스가 내려준 에러 본문 전달
            throw new IllegalArgumentException("결제 취소 실패: " + e.getResponseBodyAsString());
        }
    }

    // 토스 결제 승인 API 호출 (시크릿 키 뒤에 ':' 붙여 Base64 → Basic 인증)
    private void approvePayment(ConfirmRequest req) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", req.getOrderId(),
                "amount", req.getAmount()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(confirmUrl, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("결제 승인에 실패했습니다.");
            }
        } catch (RestClientResponseException e) {
            // 토스가 내려준 에러 본문 전달
            throw new IllegalArgumentException("결제 승인 실패: " + e.getResponseBodyAsString());
        }
    }
}
