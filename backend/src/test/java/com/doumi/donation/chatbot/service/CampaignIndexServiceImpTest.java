package com.doumi.donation.chatbot.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignIndexServiceImpTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private CampaignsDao campaignsDao;

    @InjectMocks
    private CampaignIndexServiceImp service;

    @Captor
    private ArgumentCaptor<List<Document>> docsCaptor;

    private Campaign campaign(long id) {
        Campaign c = new Campaign();
        c.setCampaignId(id);
        c.setTitle("캠페인 " + id);
        c.setStatus("모집중");
        return c;
    }

    @Test
    @DisplayName("reindexAll은 50건 단위로 나눠 색인하고 전체 건수를 반환한다")
    void reindexAllBatchesByFifty() {
        List<Campaign> campaigns = LongStream.rangeClosed(1, 120)
                .mapToObj(this::campaign)
                .toList();
        when(campaignsDao.getAllCampaigns(null, null, null, null)).thenReturn(campaigns);

        int indexed = service.reindexAll();

        assertThat(indexed).isEqualTo(120);
        verify(vectorStore, times(3)).add(docsCaptor.capture());
        assertThat(docsCaptor.getAllValues())
                .extracting(List::size)
                .containsExactly(50, 50, 20);
    }

    @Test
    @DisplayName("존재하지 않는 캠페인 단건 색인 요청은 색인 없이 무시된다")
    void indexCampaignSkipsWhenNotFound() {
        when(campaignsDao.getDetailCampaign(99L)).thenReturn(null);

        service.indexCampaign(99L);

        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("필드가 null인 캠페인도 NPE 없이 색인되고, 문서 id는 campaignId다")
    void indexCampaignHandlesNullFields() {
        Campaign c = new Campaign();
        c.setCampaignId(7L);
        // title/description/category/region/status 전부 null인 상태
        when(campaignsDao.getDetailCampaign(7L)).thenReturn(c);

        service.indexCampaign(7L);

        verify(vectorStore).add(docsCaptor.capture());
        Document doc = docsCaptor.getValue().get(0);
        assertThat(doc.getId()).isEqualTo("7");
        assertThat(doc.getMetadata().get("status")).isEqualTo("");
        assertThat(doc.getMetadata().get("campaignId")).isEqualTo(7L);
        assertThat(doc.getText()).contains("제목:");
    }

    @Test
    @DisplayName("removeCampaign은 campaignId를 문서 id로 변환해 삭제한다")
    void removeCampaignDeletesByDocumentId() {
        service.removeCampaign(7L);

        verify(vectorStore).delete(List.of("7"));
    }
}
