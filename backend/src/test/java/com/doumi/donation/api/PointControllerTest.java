package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.point.controller.PointController;
import com.doumi.donation.point.service.PointService;
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

import java.util.List;

import static com.doumi.donation.api.ApiTestSupport.로그인;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PointController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("포인트 /api/points")
class PointControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PointService pointService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/points/charge → 200 포인트 충전 (로그인)")
    void 충전() throws Exception {
        mvc.perform(post("/api/points/charge").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10000}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/points/use → 200 포인트로 기부 (로그인)")
    void 사용() throws Exception {
        mvc.perform(post("/api/points/use").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"campaignId":1,"amount":5000}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/points/history → 200 충전/사용 이력 (로그인)")
    void 이력조회() throws Exception {
        when(pointService.getHistory(1L)).thenReturn(List.of());

        mvc.perform(get("/api/points/history").with(로그인()))
                .andExpect(status().isOk());
    }
}
