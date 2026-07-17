package com.doumi.donation.chatbot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** LLM이 반환하는 구조화된 응답 (Structured Output 변환 대상) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResult {
    private String answer;
    private List<Long> recommendedCampaignIds;
}
