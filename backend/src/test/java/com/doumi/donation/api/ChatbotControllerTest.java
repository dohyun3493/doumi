package com.doumi.donation.api;

import com.doumi.donation.chatbot.controller.ChatbotController;
import com.doumi.donation.chatbot.model.dto.ChatbotResponse;
import com.doumi.donation.chatbot.service.CampaignIndexService;
import com.doumi.donation.chatbot.service.ChatbotService;
import com.doumi.donation.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatbotController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("챗봇 /api/chatbot")
class ChatbotControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ChatbotService chatbotService;
    @MockitoBean
    CampaignIndexService campaignIndexService;

    @Test
    @DisplayName("POST /api/chatbot/chat → 200 대화 + 캠페인 추천")
    void 대화() throws Exception {
        when(chatbotService.chat(any(), any()))
                .thenReturn(new ChatbotResponse("이 캠페인 어때요?", List.of()));

        mvc.perform(post("/api/chatbot/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"s1","message":"광주 아동을 돕고 싶어요"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("이 캠페인 어때요?"));
    }

    @Test
    @DisplayName("POST /api/admin/chatbot/reindex → 200 벡터 DB 전체 재색인 (관리자)")
    void 재색인() throws Exception {
        when(campaignIndexService.reindexAll()).thenReturn(10);

        mvc.perform(post("/api/admin/chatbot/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(10));
    }
}
