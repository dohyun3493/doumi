package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.payment.controller.PaymentController;
import com.doumi.donation.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.doumi.donation.api.ApiTestSupport.로그인;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("결제 /api/payments")
class PaymentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PaymentService paymentService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/payments/confirm → 200 토스 결제 승인 + 포인트 충전 (로그인)")
    void 결제승인() throws Exception {
        mvc.perform(post("/api/payments/confirm").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentKey":"pay_key","orderId":"order_1","amount":10000}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/payments → 200 내 충전(결제) 내역 조회 (로그인)")
    void 내결제내역() throws Exception {
        mvc.perform(get("/api/payments").with(로그인()))
                .andExpect(status().isOk());

        verify(paymentService).findByMemberId(eq(1L));
    }

    @Test
    @DisplayName("POST /api/payments/{id}/cancel → 200 결제 취소(환불), 본인 memberId로 호출 (로그인)")
    void 결제취소() throws Exception {
        mvc.perform(post("/api/payments/5/cancel").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"단순 변심"}"""))
                .andExpect(status().isOk());

        // 토큰의 memberId(1)와 경로의 paymentId(5)가 서비스로 전달되는지 확인
        verify(paymentService).cancel(eq(1L), eq(5L), eq("단순 변심"));
    }

    @Test
    @DisplayName("POST /api/payments/{id}/cancel → 본문 없이도 200 (사유 생략 가능)")
    void 결제취소_본문없음() throws Exception {
        mvc.perform(post("/api/payments/5/cancel").with(로그인()))
                .andExpect(status().isOk());

        verify(paymentService).cancel(eq(1L), eq(5L), eq(null));
    }
}
