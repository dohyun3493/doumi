package com.doumi.donation.chatbot.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignIndexServiceImp implements CampaignIndexService {

    private static final int BATCH_SIZE = 50;

    private final VectorStore vectorStore;
    private final CampaignsDao campaignsDao;

    @Override
    public int reindexAll() {
        List<Campaign> campaigns = campaignsDao.getAllCampaigns(null, null, null, null);
        List<Document> docs = new ArrayList<>();

        for (Campaign campaign : campaigns) {
            Document doc = this.toDocument(campaign);
            docs.add(doc);
        }

        // 임베딩 API 호출 부담을 줄이기 위해 나눠서 색인
        for (int i = 0; i < docs.size(); i += BATCH_SIZE) {
            vectorStore.add(docs.subList(i, Math.min(i + BATCH_SIZE, docs.size())));
            log.info("캠페인 벡터 색인 진행: {}/{}", Math.min(i + BATCH_SIZE, docs.size()), docs.size());
        }
        return docs.size();
    }

    // 단체가 캠페인 등록할 때 vector db에 저장
    @Override
    public void indexCampaign(long campaignId) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            log.warn("색인할 캠페인을 찾지 못함: campaignId={}", campaignId);
            return;
        }
        vectorStore.add(List.of(toDocument(campaign)));
        log.info("캠페인 단건 색인 완료: campaignId={}", campaignId);
    }

    // 단체가 캠페인 삭제할 때 vector db에서 삭제
    @Override
    public void removeCampaign(long campaignId) {
        vectorStore.delete(List.of(String.valueOf(campaignId)));
        log.info("캠페인 벡터 제거 완료: campaignId={}", campaignId);
    }

    /**
     * 캠페인 → 벡터 문서 변환.
     * - text: 의미 기반 유사도 검색 대상 (제목/단체/카테고리/지역/목적/설명)
     * - metadata: 검색 필터링용 (status 등)
     * - id를 campaignId로 고정해서 재색인 시 중복 없이 덮어쓰기(upsert)
     */
    private Document toDocument(Campaign c) {
        String content = """
                제목: %s
                기부 단체: %s
                카테고리: %s
                지역: %s
                목적: %s
                설명: %s
                """.formatted(nvl(c.getTitle()), nvl(c.getOrgName()), nvl(c.getCategory()),
                              nvl(c.getRegion()), nvl(c.getPurpose()), nvl(c.getDescription()));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("campaignId", c.getCampaignId());
        metadata.put("category", nvl(c.getCategory()));
        metadata.put("region", nvl(c.getRegion()));
        metadata.put("status", nvl(c.getStatus()));

        return Document.builder()
                .id(String.valueOf(c.getCampaignId()))
                .text(content)
                .metadata(metadata)
                .build();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
