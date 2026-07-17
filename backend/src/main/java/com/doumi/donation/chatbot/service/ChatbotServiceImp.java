package com.doumi.donation.chatbot.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.chatbot.model.dto.ChatbotResponse;
import com.doumi.donation.chatbot.model.dto.ChatbotResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotServiceImp implements ChatbotService {

    private static final String SYSTEM_PROMPT = """
            너는 기부 플랫폼의 캠페인 추천 도우미야. 사용자와 대화하면서 관심 분야(아동, 노인, 동물, 환경 등),
            지역, 돕고 싶은 대상 같은 정보를 파악하고, 아래 [후보 캠페인] 중에서 가장 적합한 캠페인을 추천해.

            규칙:
            - 사용자의 의도를 파악하기에 정보가 부족하면 추천하지 말고(recommendedCampaignIds는 빈 배열),
              관심 분야나 지역을 자연스럽게 한 가지만 되물어봐.
            - 추천할 때는 반드시 [후보 캠페인] 목록에 있는 campaignId만 사용해. 목록에 없는 캠페인을 지어내지 마.
            - 적합한 후보가 없으면 솔직하게 없다고 답하고 recommendedCampaignIds는 빈 배열로 둬.
            - 추천 시 answer에는 왜 이 캠페인이 사용자의 관심사와 맞는지 1~2문장으로 설명해.
            - 항상 한국어로, 친근하고 간결하게 답해.
            """;

    private static final int TOP_K = 5;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final CampaignsDao campaignsDao;

    public ChatbotServiceImp(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                             VectorStore vectorStore, CampaignsDao campaignsDao) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.vectorStore = vectorStore;
        this.campaignsDao = campaignsDao;
    }

    @Override
    public ChatbotResponse chat(String sessionId, String message) {
        // 1. 벡터 유사도 검색으로 후보 캠페인 추출 (모집이 끝난 캠페인은 제외)
        FilterExpressionBuilder fb = new FilterExpressionBuilder();
        Filter.Expression activeOnly = fb.not(fb.in("status", "모집완료", "전달완료", "사용완료")).build();

        List<Document> candidates = vectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .topK(TOP_K)
                .filterExpression(activeOnly)
                .build());

        String context = candidates.isEmpty()
                ? "(검색된 후보 캠페인이 없음)"
                : candidates.stream()
                        .map(doc -> "campaignId: %s\n%s".formatted(doc.getId(), doc.getText()))
                        .collect(Collectors.joining("\n---\n"));

        // 2. 후보를 컨텍스트로 LLM 호출 → 구조화된 응답(답변 + 추천 ID)으로 변환
        //    후보 목록은 시스템 메시지에 넣어 대화 기록(ChatMemory)에는 사용자 메시지만 남게 함
        ChatbotResult result = chatClient.prompt()
                .system(SYSTEM_PROMPT + "\n[후보 캠페인]\n" + context)
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .entity(ChatbotResult.class);

        // 3. LLM이 고른 ID를 후보 목록으로 검증한 뒤, DB에서 최신 캠페인 정보 조회
        //    (모금액/상태는 수시로 바뀌므로 벡터 저장소가 아닌 DB 값이 정답)
        List<Campaign> campaigns = findCampaigns(result, candidates);

        return new ChatbotResponse(result.getAnswer(), campaigns);
    }

    // 테스트 접근을 위해 package-private (LLM 응답 검증 로직은 단위 테스트 대상)
    List<Campaign> findCampaigns(ChatbotResult result, List<Document> candidates) {
        if (result.getRecommendedCampaignIds() == null || result.getRecommendedCampaignIds().isEmpty()) {
            return List.of();
        }

        Set<String> candidateIds = candidates.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());

        List<Long> validIds = result.getRecommendedCampaignIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(id -> candidateIds.contains(String.valueOf(id)))
                .toList();

        if (validIds.isEmpty()) {
            log.warn("LLM이 후보에 없는 campaignId를 반환함: {}", result.getRecommendedCampaignIds());
            return List.of();
        }

        Map<Long, Campaign> byId = campaignsDao.getCampaignsByIds(validIds).stream()
                .collect(Collectors.toMap(Campaign::getCampaignId, Function.identity()));

        // LLM이 추천한 순서 유지
        return validIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
