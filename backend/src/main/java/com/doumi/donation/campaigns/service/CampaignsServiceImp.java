package com.doumi.donation.campaigns.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.model.dto.DonationDto;
import com.doumi.donation.chatbot.service.CampaignIndexService;
import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.notification.model.dao.NotificationDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CampaignsServiceImp implements CampaignsService {
    private final CampaignsDao campaignsDao;
    private final CampaignIndexService campaignIndexService;
    private final MemberDao memberDao;
    private final NotificationDao notificationDao;

    @Autowired
    CampaignsServiceImp(CampaignsDao campaignsDao, CampaignIndexService campaignIndexService,
                        MemberDao memberDao, NotificationDao notificationDao){
        this.campaignsDao = campaignsDao;
        this.campaignIndexService = campaignIndexService;
        this.memberDao = memberDao;
        this.notificationDao = notificationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campaign> getAllCampaigns(String category, String region, String status, String keyword) {
        return campaignsDao.getAllCampaigns(category, region, status, keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public Campaign getDetailCampaign(long id) {
        return campaignsDao.getDetailCampaign(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campaign> getMyCampaigns(long memberId) {
        return campaignsDao.getCampaignsByOwner(memberId);
    }

    @Override
    @Transactional
    public void registCampaign(Campaign req){
        // 단체 회원은 관리자 승인(APPROVED) 후에만 캠페인 등록 가능
        // (JWT가 아닌 DB의 최신 승인 상태로 검증 — 토큰 발급 후 거절된 경우도 차단)
        if (req.getOwnerId() != null) {
            Member owner = memberDao.findByMemberId(req.getOwnerId());
            if (owner != null && "ORGANIZATION".equals(owner.getMemberType())
                    && !"APPROVED".equals(owner.getOrgStatus())) {
                throw new UnauthorizedException("관리자 승인이 완료된 단체만 캠페인을 등록할 수 있습니다.");
            }
        }

        campaignsDao.registCampaign(req);
        // useGeneratedKeys로 채워진 campaignId로 챗봇 벡터 DB에 즉시 색인
        syncVectorStore(() -> campaignIndexService.indexCampaign(req.getCampaignId()));
    }

    @Override
    @Transactional
    public void deleteCampaing(long campaignId, long memberId){
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        verifyOwnerOrAdmin(campaign, memberId);
        campaignsDao.deleteCampaign(campaignId);
        syncVectorStore(() -> campaignIndexService.removeCampaign(campaignId));
    }

    @Override
    @Transactional
    public void updateCampaign(long campaignId, long memberId, Campaign req){
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        verifyOwnerOrAdmin(campaign, memberId);
        campaignsDao.updateCampaign(campaignId, req);
        // 제목/설명/상태 등이 바뀌었을 수 있으므로 벡터 DB에 덮어쓰기
        syncVectorStore(() -> campaignIndexService.indexCampaign(campaignId));
    }

    // 객체 레벨 인가: 캠페인 소유 단체 본인 또는 관리자만 수정·삭제 가능
    // (URL 레벨에서 ORGANIZATION/ADMIN까지만 걸러지므로, 다른 단체가 남의 캠페인을
    //  수정하는 것은 여기서 차단. 공공데이터 수집 캠페인은 owner_id가 null → 관리자만 가능)
    private void verifyOwnerOrAdmin(Campaign campaign, long memberId) {
        Member member = memberDao.findByMemberId(memberId);
        if (member != null && "ADMIN".equals(member.getMemberType())) {
            return;
        }
        if (campaign.getOwnerId() == null || campaign.getOwnerId() != memberId) {
            throw new UnauthorizedException("본인이 등록한 캠페인만 수정·삭제할 수 있습니다.");
        }
    }

    @Override
    @Transactional
    public void requestDeletion(long campaignId, long memberId, String reason) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        // 소유 단체 본인만 삭제 요청 가능 (공공데이터 캠페인은 ownerId가 null)
        if (campaign.getOwnerId() == null || campaign.getOwnerId() != memberId) {
            throw new UnauthorizedException("본인이 등록한 캠페인만 삭제 요청할 수 있습니다.");
        }
        if (campaign.isDeleteRequested()) {
            throw new DuplicateResourceException("이미 삭제 요청된 캠페인입니다.");
        }
        campaignsDao.updateDeleteRequest(campaignId, true, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Campaign> getDeleteRequestedCampaigns() {
        return campaignsDao.findDeleteRequested();
    }

    @Override
    @Transactional
    public void approveDeletion(long campaignId) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        if (!campaign.isDeleteRequested()) {
            throw new IllegalArgumentException("삭제 요청된 캠페인이 아닙니다.");
        }
        // 기부 내역이 있으면 회계 기록 보존을 위해 삭제 불가 (FK 보호)
        if (campaignsDao.countDonationsByCampaignId(campaignId) > 0) {
            throw new IllegalArgumentException("기부 내역이 있는 캠페인은 삭제할 수 없습니다.");
        }

        Long ownerId = campaign.getOwnerId();   // 삭제 전에 알림 대상 확보
        String title = campaign.getTitle();

        campaignsDao.deleteCampaign(campaignId);
        syncVectorStore(() -> campaignIndexService.removeCampaign(campaignId));

        if (ownerId != null) {
            notifyMember(ownerId, "CAMPAIGN_DELETE_APPROVED",
                    "요청하신 캠페인 \"" + title + "\" 삭제가 승인되어 삭제되었습니다.", null);
        }
    }

    @Override
    @Transactional
    public void rejectDeletion(long campaignId, String reason) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        if (!campaign.isDeleteRequested()) {
            throw new IllegalArgumentException("삭제 요청된 캠페인이 아닙니다.");
        }
        campaignsDao.updateDeleteRequest(campaignId, false, null);   // 요청 해제

        if (campaign.getOwnerId() != null) {
            String content = "요청하신 캠페인 \"" + campaign.getTitle() + "\" 삭제가 거부되었습니다."
                    + (reason != null && !reason.isBlank() ? " 사유: " + reason : "");
            notifyMember(campaign.getOwnerId(), "CAMPAIGN_DELETE_REJECTED",
                    content, "/campaigns/" + campaignId);
        }
    }

    // 알림 실패가 본 작업(삭제/거부)을 실패시키지 않도록 격리
    private void notifyMember(long memberId, String type, String content, String linkUrl) {
        try {
            notificationDao.insertNotification(memberId, type, content, linkUrl);
        } catch (Exception e) {
            log.error("캠페인 삭제 처리 알림 생성 실패: memberId={}", memberId, e);
        }
    }

    // 벡터 DB 동기화 실패가 캠페인 등록/수정/삭제 자체를 실패시키지 않도록 격리
    // (실패 시 다음 배치 재색인이나 관리자 reindex API로 복구됨)
    private void syncVectorStore(Runnable sync) {
        try {
            sync.run();
        } catch (Exception e) {
            log.error("챗봇 벡터 DB 동기화 실패", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationDto> getDonationsByCampaignId(long campaignId) {
        return campaignsDao.getDonationsByCampaignId(campaignId);
    }

    @Override
    @Transactional
    public int closeExpiredCampaigns() {
        // 전환될 캠페인 id를 먼저 확보 (벡터 DB에서 제거하기 위함)
        List<Long> expiredIds = campaignsDao.findExpiredOpenCampaignIds();
        if (expiredIds.isEmpty()) {
            return 0;
        }
        int closed = campaignsDao.closeExpiredCampaigns();
        // 모집완료된 캠페인은 챗봇 추천 후보에서 빠지도록 벡터 DB에서 제거
        // (제거는 임베딩 호출이 없어 가볍고, 실패해도 본 작업에 영향 없도록 격리)
        expiredIds.forEach(id -> syncVectorStore(() -> campaignIndexService.removeCampaign(id)));
        return closed;
    }

    @Override
    @Transactional
    public void deliverCampaign(long campaignId) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        String status = campaign.getStatus();
        if ("전달완료".equals(status) || "사용완료".equals(status)) {
            throw new IllegalArgumentException("이미 전달 처리된 캠페인입니다.");
        }
        // 모집완료(마감)이거나 목표 금액을 달성한 캠페인만 전달 가능
        boolean goalReached = campaign.getGoalAmount() != null && campaign.getCurrentAmount() != null
                && campaign.getCurrentAmount() >= campaign.getGoalAmount();
        if (!"모집완료".equals(status) && !goalReached) {
            throw new IllegalArgumentException("모집이 완료됐거나 목표를 달성한 캠페인만 전달할 수 있습니다.");
        }

        Campaign update = new Campaign();
        update.setStatus("전달완료");
        campaignsDao.updateCampaign(campaignId, update);   // 상태만 변경(다른 필드 null → 미수정)

        // 전달완료 캠페인은 챗봇 추천 후보에서 제외 (제거는 임베딩 호출 없이 가벼움)
        syncVectorStore(() -> campaignIndexService.removeCampaign(campaignId));
    }

    @Override
    @Transactional
    public int openScheduledCampaigns() {
        // 시작일 도래한 '모집예정'을 '모집중'으로 전환 (DB만 갱신).
        // 모집예정 캠페인은 등록 시 이미 벡터 DB에 색인돼 있고, 챗봇 필터는 모집예정/모집중을
        // 모두 추천 대상으로 보므로 별도 벡터 동기화가 필요 없다(임베딩 호출도 아낌).
        return campaignsDao.openScheduledCampaigns();
    }
}
