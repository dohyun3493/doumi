package com.doumi.donation.campaigns.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.chatbot.service.CampaignIndexService;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignsServiceImpTest {

    @Mock
    private CampaignsDao campaignsDao;

    @Mock
    private CampaignIndexService campaignIndexService;

    @Mock
    private MemberDao memberDao;

    @InjectMocks
    private CampaignsServiceImp service;

    // 소유 단체(owner_id) 캠페인 생성 헬퍼
    private Campaign campaignOwnedBy(Long ownerId) {
        Campaign c = new Campaign();
        c.setOwnerId(ownerId);
        return c;
    }

    @Test
    @DisplayName("캠페인 등록 시 DB가 생성한 campaignId로 벡터 색인을 호출한다")
    void registIndexesWithGeneratedId() {
        // useGeneratedKeys가 INSERT 후 DTO에 id를 채워주는 동작을 재현
        doAnswer(inv -> {
            inv.getArgument(0, Campaign.class).setCampaignId(42L);
            return null;
        }).when(campaignsDao).registCampaign(any(Campaign.class));

        service.registCampaign(new Campaign());

        verify(campaignIndexService).indexCampaign(42L);
    }

    @Test
    @DisplayName("벡터 색인이 실패해도 캠페인 등록은 성공한다 (동기화 격리)")
    void registSucceedsEvenIfIndexingFails() {
        doAnswer(inv -> {
            inv.getArgument(0, Campaign.class).setCampaignId(42L);
            return null;
        }).when(campaignsDao).registCampaign(any(Campaign.class));
        doThrow(new RuntimeException("Redis down")).when(campaignIndexService).indexCampaign(anyLong());

        assertThatCode(() -> service.registCampaign(new Campaign()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("존재하지 않는 캠페인 수정은 ResourceNotFoundException이며 색인하지 않는다")
    void updateNonexistentThrowsAndSkipsIndexing() {
        when(campaignsDao.getDetailCampaign(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateCampaign(1L, 100L, new Campaign()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(campaignsDao, never()).updateCampaign(anyLong(), any());
        verify(campaignIndexService, never()).indexCampaign(anyLong());
    }

    @Test
    @DisplayName("소유 단체 본인이 수정하면 성공하고 벡터 DB에 덮어쓰기 색인한다")
    void updateReindexesCampaign() {
        when(campaignsDao.getDetailCampaign(5L)).thenReturn(campaignOwnedBy(100L));

        service.updateCampaign(5L, 100L, new Campaign());

        verify(campaignsDao).updateCampaign(eq(5L), any(Campaign.class));
        verify(campaignIndexService).indexCampaign(5L);
    }

    @Test
    @DisplayName("다른 단체가 남의 캠페인을 수정하면 UnauthorizedException이며 DB·색인을 건드리지 않는다")
    void updateByNonOwnerThrows() {
        when(campaignsDao.getDetailCampaign(5L)).thenReturn(campaignOwnedBy(100L));
        // 요청자(200)는 관리자가 아님
        Member other = new Member();
        other.setMemberType("ORGANIZATION");
        when(memberDao.findByMemberId(200L)).thenReturn(other);

        assertThatThrownBy(() -> service.updateCampaign(5L, 200L, new Campaign()))
                .isInstanceOf(UnauthorizedException.class);

        verify(campaignsDao, never()).updateCampaign(anyLong(), any());
        verify(campaignIndexService, never()).indexCampaign(anyLong());
    }

    @Test
    @DisplayName("관리자는 공공데이터(owner_id=null) 캠페인도 수정할 수 있다")
    void adminCanUpdatePublicDataCampaign() {
        when(campaignsDao.getDetailCampaign(7L)).thenReturn(campaignOwnedBy(null));
        Member admin = new Member();
        admin.setMemberType("ADMIN");
        when(memberDao.findByMemberId(1L)).thenReturn(admin);

        service.updateCampaign(7L, 1L, new Campaign());

        verify(campaignsDao).updateCampaign(eq(7L), any(Campaign.class));
        verify(campaignIndexService).indexCampaign(7L);
    }

    @Test
    @DisplayName("소유 단체 본인이 삭제하면 벡터도 함께 제거한다")
    void deleteRemovesVector() {
        when(campaignsDao.getDetailCampaign(9L)).thenReturn(campaignOwnedBy(100L));

        service.deleteCampaing(9L, 100L);

        verify(campaignsDao).deleteCampaign(9L);
        verify(campaignIndexService).removeCampaign(9L);
    }
}
