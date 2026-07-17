package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.ranking.controller.RankingController;
import com.doumi.donation.ranking.service.RankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RankingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("랭킹 /api/rankings")
class RankingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RankingService rankingService;

    @Test
    @DisplayName("GET /api/rankings?yearMonth= → 200 월별 기부 랭킹")
    void 랭킹조회() throws Exception {
        when(rankingService.getTopRankings(eq("2026-06"), anyInt())).thenReturn(List.of());

        mvc.perform(get("/api/rankings").param("yearMonth", "2026-06"))
                .andExpect(status().isOk());
    }
}
