package com.doumi.donation.chatbot.service;

public interface CampaignIndexService {
    /** 전체 캠페인을 Vector DB에 색인(upsert). 색인된 건수 반환 */
    int reindexAll();

    /** 단건 캠페인을 Vector DB에 색인(upsert). 등록/수정 직후 호출 */
    void indexCampaign(long campaignId);

    /** Vector DB에서 캠페인 벡터 제거. 삭제 직후 호출 */
    void removeCampaign(long campaignId);
}
