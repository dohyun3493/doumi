package com.doumi.donation.payment.client;

import com.doumi.donation.exception.PaymentDeclinedException;
import com.doumi.donation.exception.PaymentPendingException;
import com.doumi.donation.payment.model.dto.ConfirmRequest;
import com.doumi.donation.payment.model.dto.TossPaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 HTTP 호출만 담당한다.
 *
 * <p>결제 서비스에서 외부 I/O를 분리해 둔 이유는, 이 클래스의 메서드가
 * <b>절대 DB 트랜잭션 안에서 호출되면 안 된다</b>는 것을 구조로 드러내기 위함이다.
 * 트랜잭션 안에서 호출하면 응답을 기다리는 동안 DB 커넥션이 반납되지 않아
 * 커넥션 풀이 고갈되고, 결제와 무관한 API까지 함께 죽는다.
 *
 * <p>실패를 두 종류로 구분해 던지는 것이 이 클래스의 핵심 책임이다.
 * <ul>
 *   <li>{@link PaymentDeclinedException} — 토스가 4xx로 거절. <b>결과 확정</b>이라 즉시 종결 가능</li>
 *   <li>{@link PaymentPendingException} — 타임아웃·5xx. <b>결과 불명</b>이라 나중에 재조회해야 함</li>
 * </ul>
 * 이 둘을 뭉뚱그리면, 실제로는 승인된 결제를 실패로 처리해 돈만 빠지는 사고가 난다.
 */
@Component
public class TossPaymentClient {

    private final RestTemplate restTemplate = createRestTemplate();

    // new RestTemplate()은 기본 타임아웃이 없어 응답이 없으면 무한정 대기한다.
    // 상한을 두지 않으면 커넥션이 영영 반납되지 않으므로 반드시 설정한다.
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

    // 결제 조회/취소 공통 베이스 URL
    // (조회: GET {base}/{paymentKey}, 취소: POST {base}/{paymentKey}/cancel)
    @Value("${toss.payments-url:${toss.cancel-url}}")
    private String paymentsBaseUrl;

    /** 토스 결제 승인. 성공하면 되돌릴 수 없으므로 호출 시점이 곧 확정 시점이다. */
    public void approve(ConfirmRequest req) {
        Map<String, Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", req.getOrderId(),
                "amount", req.getAmount()
        );

        try {
            restTemplate.postForEntity(confirmUrl, new HttpEntity<>(body, authHeaders()), Map.class);
        } catch (RestClientResponseException e) {
            throw toFailure(e, "결제 승인");
        } catch (RuntimeException e) {
            // 타임아웃·커넥션 오류 → 승인 여부를 알 수 없다
            throw new PaymentPendingException("결제 승인 결과를 확인하지 못했습니다: " + e.getMessage());
        }
    }

    /** 토스 결제 취소: POST {base}/{paymentKey}/cancel  body: { cancelReason } */
    public void cancel(String paymentKey, String reason) {
        Map<String, Object> body = Map.of(
                "cancelReason", (reason == null || reason.isBlank()) ? "사용자 요청" : reason
        );

        try {
            restTemplate.postForEntity(paymentsBaseUrl + "/" + paymentKey + "/cancel",
                    new HttpEntity<>(body, authHeaders()), Map.class);
        } catch (RestClientResponseException e) {
            throw toFailure(e, "결제 취소");
        } catch (RuntimeException e) {
            throw new PaymentPendingException("결제 취소 결과를 확인하지 못했습니다: " + e.getMessage());
        }
    }

    /**
     * 토스 결제 조회. 결과를 모르는 건(PENDING/CANCELING)의 <b>진짜</b> 상태를 확인할 때 쓴다.
     * 조회 자체가 실패하면 판단을 미뤄야 하므로 {@link PaymentPendingException}을 던진다.
     */
    public TossPaymentStatus findStatus(String paymentKey) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    paymentsBaseUrl + "/" + paymentKey,
                    HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

            Object status = (response.getBody() == null) ? null : response.getBody().get("status");
            return new TossPaymentStatus(status == null ? null : status.toString());
        } catch (HttpClientErrorException.NotFound e) {
            // 토스에 결제 자체가 없음 → 승인 요청이 도달하지 못한 것으로 확정
            return TossPaymentStatus.notFound();
        } catch (RuntimeException e) {
            throw new PaymentPendingException("결제 조회에 실패했습니다: " + e.getMessage());
        }
    }

    // 4xx는 토스가 판단해 거절한 것이므로 확정 실패,
    // 5xx는 토스 내부 오류라 처리됐는지 알 수 없으므로 보류로 나눈다.
    private RuntimeException toFailure(RestClientResponseException e, String action) {
        String detail = action + " 실패: " + e.getResponseBodyAsString();
        return e.getStatusCode().is4xxClientError()
                ? new PaymentDeclinedException(detail)
                : new PaymentPendingException(detail);
    }

    // 시크릿 키 뒤에 ':'를 붙여 Base64 → Basic 인증
    private HttpHeaders authHeaders() {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }
}
