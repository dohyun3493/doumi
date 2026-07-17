package com.doumi.donation.chatbot.model.dto;

import com.doumi.donation.campaigns.model.dto.Campaign;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** 프론트로 내려주는 최종 응답: 답변 텍스트 + 추천 캠페인 (DB 최신값) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String answer;
    private List<Campaign> campaigns;
}
