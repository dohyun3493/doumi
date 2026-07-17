package com.doumi.donation.report.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gemini API(generativelanguage) 경로를 호출하는 클라이언트.
 * 인증은 x-goog-api-key 헤더로 전달한다. 텍스트 + 이미지(멀티모달) 입력과
 * JSON 구조화 출력을 지원한다.
 */
@Component
public class GeminiClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("${gemini.base-url}") String baseUrl,
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model}") String model,
                        ObjectMapper objectMapper) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    /** 멀티모달 입력(텍스트 + 이미지)을 보내고, responseSchema에 맞춘 JSON 응답을 파싱해 반환한다. */
    public JsonNode generateJson(String prompt, List<InlineImage> images, Map<String, Object> responseSchema) {
        List<Object> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        if (images != null) {
            for (InlineImage img : images) {
                parts.add(Map.of("inline_data", Map.of(
                        "mime_type", img.mimeType(),
                        "data", Base64.getEncoder().encodeToString(img.bytes()))));
            }
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema));

        String text = callAndExtractText(body);
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException("AI 응답(JSON)을 해석할 수 없습니다.", e);
        }
    }

    private String callAndExtractText(Map<String, Object> body) {
        JsonNode res = client.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (res == null) {
            throw new IllegalStateException("AI 응답이 비어 있습니다.");
        }
        JsonNode partsNode = res.path("candidates").path(0).path("content").path("parts");
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : partsNode) {
            sb.append(p.path("text").asText(""));
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("AI가 응답을 생성하지 못했습니다.");
        }
        return text;
    }

    /** 멀티모달 입력 이미지 (mime + 원본 바이트) */
    public record InlineImage(String mimeType, byte[] bytes) {}
}
