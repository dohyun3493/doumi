package com.doumi.donation.api;

import com.doumi.donation.campaigns.controller.CampaignsController;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.service.CampaignsService;
import com.doumi.donation.config.SecurityConfig;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CampaignsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("캠페인 /api/campaigns")
class CampaignsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CampaignsService campaignsService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    private Campaign 캠페인하나() {
        Campaign c = new Campaign();
        c.setCampaignId(1L);
        c.setTitle("아동 급식 지원");
        c.setStatus("모집중");
        return c;
    }

    @Test
    @DisplayName("GET /api/campaigns → 200 목록 조회(필터 가능)")
    void 목록조회() throws Exception {
        when(campaignsService.getAllCampaigns(any(), any(), any(), any()))
                .thenReturn(List.of(캠페인하나()));

        mvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("아동 급식 지원"));
    }

    @Test
    @DisplayName("GET /api/campaigns/{id} → 200 상세 조회")
    void 상세조회() throws Exception {
        when(campaignsService.getDetailCampaign(1L)).thenReturn(캠페인하나());

        mvc.perform(get("/api/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(1));
    }

    @Test
    @DisplayName("POST /api/campaigns → 201 캠페인 등록, 등록자가 소유자로 기록됨 (단체/관리자)")
    void 등록() throws Exception {
        mvc.perform(post("/api/campaigns").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"새 캠페인","category":"아동","region":"광주"}"""))
                .andExpect(status().isCreated());

        // 토큰의 memberId(1)가 ownerId로 들어갔는지 확인
        verify(campaignsService).registCampaign(argThat(c -> Long.valueOf(1L).equals(c.getOwnerId())));
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} → 200 캠페인 수정 (소유 단체 본인, memberId 전달)")
    void 수정() throws Exception {
        mvc.perform(patch("/api/campaigns/1").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"수정된 제목"}"""))
                .andExpect(status().isOk());

        // 토큰의 memberId(1)가 객체 레벨 인가용으로 서비스에 전달됐는지 확인
        verify(campaignsService).updateCampaign(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(1L), any(Campaign.class));
    }

    @Test
    @DisplayName("DELETE /api/campaigns/{id} → 200 캠페인 삭제 (memberId 전달)")
    void 삭제() throws Exception {
        mvc.perform(delete("/api/campaigns/1").with(로그인()))
                .andExpect(status().isOk());

        verify(campaignsService).deleteCampaing(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    @DisplayName("GET /api/campaigns/{id}/donations → 200 캠페인 기부 내역")
    void 기부내역() throws Exception {
        when(campaignsService.getDonationsByCampaignId(1L)).thenReturn(List.of());

        mvc.perform(get("/api/campaigns/1/donations"))
                .andExpect(status().isOk());
    }
}
