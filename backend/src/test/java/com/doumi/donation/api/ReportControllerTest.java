package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.report.controller.ReportController;
import com.doumi.donation.report.model.dto.CampaignReport;
import com.doumi.donation.report.service.ReportService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("기부금 사용 보고 /api/campaigns/{campaignId}/report")
class ReportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReportService reportService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/campaigns/{campaignId}/report → 200 사용 보고 조회 (공개)")
    void 보고조회() throws Exception {
        CampaignReport report = new CampaignReport();
        report.setCampaignId(1L);
        report.setContent("급식비로 사용했습니다.");
        when(reportService.getReport(1L)).thenReturn(report);

        mvc.perform(get("/api/campaigns/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(1));
    }

    @Test
    @DisplayName("POST /api/campaigns/{campaignId}/report → 201 사용 보고 작성 (소유 단체/관리자)")
    void 보고작성() throws Exception {
        mvc.perform(post("/api/campaigns/1/report").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "모금액을 아동 급식비로 사용했습니다.",
                                  "expenses": [
                                    {"item": "급식비", "amount": 800000},
                                    {"item": "운영비", "amount": 200000}
                                  ]
                                }"""))
                .andExpect(status().isCreated());
        verify(reportService).createReport(eq(1L), eq(1L), any());
    }

    @Test
    @DisplayName("POST /api/campaigns/{campaignId}/report → 400 지출 항목 없으면 검증 실패")
    void 보고작성_검증실패() throws Exception {
        mvc.perform(post("/api/campaigns/1/report").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "", "expenses": []}"""))
                .andExpect(status().isBadRequest());
    }
}
