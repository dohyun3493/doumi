package com.doumi.donation.publicdata.model.dao;

import com.doumi.donation.publicdata.model.dto.DonationProgramDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PublicDataDao {

    void upsertCampaignBatch(List<DonationProgramDto> list);

    // 전체 리로드용 초기화 (FK 순서상 기부 → 캠페인 순으로 삭제)
    void deleteAllDonations();

    void deleteAllCampaigns();
}
