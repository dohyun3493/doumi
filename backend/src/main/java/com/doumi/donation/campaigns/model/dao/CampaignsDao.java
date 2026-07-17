package com.doumi.donation.campaigns.model.dao;

import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.model.dto.DonationDto;
import com.doumi.donation.campaigns.model.dto.DonorRefundDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CampaignsDao {
    List<Campaign> getAllCampaigns(
        @Param("category") String category,
        @Param("region") String region,
        @Param("status") String status,
        @Param("keyword") String keyword
    );
    Campaign getDetailCampaign(@Param("id") long id);
    List<Campaign> getCampaignsByIds(@Param("ids") List<Long> ids);
    /** 특정 단체(member)가 등록한 캠페인 목록 (마이페이지 '나의 캠페인') */
    List<Campaign> getCampaignsByOwner(@Param("ownerId") long ownerId);
    void registCampaign(Campaign req);
    void deleteCampaign(@Param("campaignId") long id);
    void updateCampaign(@Param("campaignId") long campaignId, @Param("req") Campaign req);
    List<DonationDto> getDonationsByCampaignId(@Param("campaignId") long campaignId);

    /** 마감일이 지난 '모집중' 캠페인 id 목록 (벡터 DB 동기화 대상) */
    List<Long> findExpiredOpenCampaignIds();
    /** 마감일이 지난 '모집중' 캠페인을 '모집완료'로 일괄 전환. 전환된 건수 반환 */
    int closeExpiredCampaigns();
    /** 시작일이 도래한 '모집예정' 캠페인을 '모집중'으로 일괄 전환. 전환된 건수 반환 */
    int openScheduledCampaigns();

    // 삭제 요청 워크플로
    /** 삭제 요청 플래그/사유 설정 (요청 true+사유 / 거부 시 false+null) */
    int updateDeleteRequest(@Param("campaignId") long campaignId,
                            @Param("requested") boolean requested,
                            @Param("reason") String reason);
    /** 삭제 요청된 캠페인 목록 */
    List<Campaign> findDeleteRequested();
    /** 해당 캠페인의 기부 건수 (삭제 가능 여부 판단용) */
    int countDonationsByCampaignId(@Param("campaignId") long campaignId);

    // ===== 단체 탈퇴 정리 =====
    /** 단체 소유 캠페인 중 '사용완료'가 아닌 캠페인의 기부를 기부자·캠페인별로 합산 (환불 대상) */
    List<DonorRefundDto> sumRefundableDonationsByOwner(@Param("ownerId") long ownerId);
    /** 단체 소유 캠페인에 달린 기부 전체 삭제 (캠페인 삭제 전 FK 해소) */
    void deleteDonationsByOwner(@Param("ownerId") long ownerId);
    /** 단체 소유 캠페인 전체 삭제 */
    void deleteCampaignsByOwner(@Param("ownerId") long ownerId);
}
