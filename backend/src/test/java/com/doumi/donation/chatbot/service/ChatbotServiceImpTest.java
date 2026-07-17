package com.doumi.donation.chatbot.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.chatbot.model.dto.ChatbotResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LLM이 반환한 추천 campaignId를 검증하는 로직 테스트.
 * LLM이 후보에 없는 ID를 지어내도(환각) 사용자에게 노출되지 않아야 한다.
 */
class ChatbotServiceImpTest {

    private VectorStore vectorStore;
    private CampaignsDao campaignsDao;
    private ChatbotServiceImp service;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        campaignsDao = mock(CampaignsDao.class);

        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.defaultAdvisors(any(Advisor[].class))).thenReturn(builder);
        when(builder.build()).thenReturn(mock(ChatClient.class));

        service = new ChatbotServiceImp(builder, mock(ChatMemory.class), vectorStore, campaignsDao);
    }

    private Document doc(long id) {
        return Document.builder().id(String.valueOf(id)).text("캠페인 " + id).build();
    }

    private Campaign campaign(long id) {
        Campaign c = new Campaign();
        c.setCampaignId(id);
        return c;
    }

    @Test
    @DisplayName("LLM이 후보에 없는 ID를 반환하면(환각) 빈 결과를 돌려준다")
    void hallucinatedIdsAreFiltered() {
        ChatbotResult result = new ChatbotResult("추천!", List.of(99L));

        List<Campaign> campaigns = service.findCampaigns(result, List.of(doc(1), doc(2)));

        assertThat(campaigns).isEmpty();
        verify(campaignsDao, never()).getCampaignsByIds(anyList());
    }

    @Test
    @DisplayName("추천 ID가 null이거나 비어 있으면 DB 조회 없이 빈 결과를 돌려준다")
    void nullOrEmptyIdsReturnEmpty() {
        assertThat(service.findCampaigns(new ChatbotResult("답", null), List.of(doc(1)))).isEmpty();
        assertThat(service.findCampaigns(new ChatbotResult("답", List.of()), List.of(doc(1)))).isEmpty();
        verify(campaignsDao, never()).getCampaignsByIds(anyList());
    }

    @Test
    @DisplayName("유효한 ID만 걸러서 LLM이 추천한 순서대로 반환한다")
    void validIdsKeepLlmOrder() {
        // LLM이 [3, 1, 99] 추천 → 99는 후보에 없으므로 제외, DB는 [1, 3] 순서로 반환해도
        // 최종 결과는 LLM 추천 순서인 [3, 1]이어야 함
        ChatbotResult result = new ChatbotResult("추천!", List.of(3L, 1L, 99L));
        List<Document> candidates = List.of(doc(1), doc(2), doc(3));
        when(campaignsDao.getCampaignsByIds(List.of(3L, 1L)))
                .thenReturn(List.of(campaign(1), campaign(3)));

        List<Campaign> campaigns = service.findCampaigns(result, candidates);

        assertThat(campaigns).extracting(Campaign::getCampaignId).containsExactly(3L, 1L);
    }

    @Test
    @DisplayName("중복 추천 ID는 한 번만 조회한다")
    void duplicateIdsAreDeduplicated() {
        ChatbotResult result = new ChatbotResult("추천!", List.of(1L, 1L, 2L));
        when(campaignsDao.getCampaignsByIds(List.of(1L, 2L)))
                .thenReturn(List.of(campaign(1), campaign(2)));

        List<Campaign> campaigns = service.findCampaigns(result, List.of(doc(1), doc(2)));

        assertThat(campaigns).extracting(Campaign::getCampaignId).containsExactly(1L, 2L);
    }
}
