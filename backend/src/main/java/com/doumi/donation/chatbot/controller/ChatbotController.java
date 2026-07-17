package com.doumi.donation.chatbot.controller;

import com.doumi.donation.chatbot.model.dto.ChatRequest;
import com.doumi.donation.chatbot.model.dto.ChatbotResponse;
import com.doumi.donation.chatbot.service.CampaignIndexService;
import com.doumi.donation.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final CampaignIndexService campaignIndexService;

    public ChatbotController(ChatbotService chatbotService, CampaignIndexService campaignIndexService) {
        this.chatbotService = chatbotService;
        this.campaignIndexService = campaignIndexService;
    }

    @PostMapping("/api/chatbot/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        try {
            ChatbotResponse response = chatbotService.chat(request.getSessionId(), request.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("챗봇 응답 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    /** 전체 캠페인 벡터 재색인 (관리자 전용 운영 작업) */
    @PostMapping("/api/admin/chatbot/reindex")
    public ResponseEntity<?> reindex() {
        int indexed = campaignIndexService.reindexAll();
        return ResponseEntity.ok(Map.of("indexed", indexed));
    }
}
