package com.doumi.donation.chatbot.service;

import com.doumi.donation.chatbot.model.dto.ChatbotResponse;

public interface ChatbotService {
    ChatbotResponse chat(String sessionId, String message);
}
