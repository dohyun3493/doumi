package com.doumi.donation.campaigns.service;

import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.model.dto.DonationDto;
import java.util.List;

public interface CampaignsService {
    public List<Campaign> getAllCampaigns(String category, String region, String status, String keyword);
    public Campaign getDetailCampaign(long id);
    /** 마이페이지: 로그인한 단체가 등록한 캠페인 목록 */
    public List<Campaign> getMyCampaigns(long memberId);
    public void registCampaign(Campaign req);
    public void deleteCampaing(long campaignId, long memberId);
    public void updateCampaign(long campaignId, long memberId, Campaign req);
    public List<DonationDto> getDonationsByCampaignId(long campaignId);
    /** 마감일이 지난 캠페인을 '모집완료'로 자동 전환 (스케줄러용). 전환 건수 반환 */
    int closeExpiredCampaigns();
    /** 시작일이 도래한 '모집예정' 캠페인을 '모집중'으로 자동 전환 (스케줄러용). 전환 건수 반환 */
    int openScheduledCampaigns();
    /** 관리자: 기부금 전달 처리 (모집완료/목표달성 → '전달완료') */
    void deliverCampaign(long campaignId);

    // 삭제 요청 워크플로
    /** 단체(소유자)가 캠페인 삭제 요청 */
    void requestDeletion(long campaignId, long memberId, String reason);
    /** 관리자: 삭제 요청된 캠페인 목록 */
    List<Campaign> getDeleteRequestedCampaigns();
    /** 관리자: 삭제 요청 승인 (실제 삭제 + 소유자 알림) */
    void approveDeletion(long campaignId);
    /** 관리자: 삭제 요청 거부 (요청 해제 + 소유자 알림) */
    void rejectDeletion(long campaignId, String reason);
}
