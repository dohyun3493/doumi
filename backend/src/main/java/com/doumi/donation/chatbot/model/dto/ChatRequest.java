package com.doumi.donation.chatbot.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 챗봇 대화 요청 (sessionId로 멀티턴 대화 구분) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @NotBlank
    private String sessionId;
    @NotBlank
    private String message;
}
