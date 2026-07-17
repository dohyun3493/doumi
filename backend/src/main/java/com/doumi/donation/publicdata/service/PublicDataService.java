package com.doumi.donation.publicdata.service;

import com.doumi.donation.publicdata.model.dao.PublicDataDao;
import com.doumi.donation.publicdata.model.dto.DonationProgramDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

@Service
public class PublicDataService {

    private final RestClient restClient;
    private final PublicDataDao publicDataDao;

    @Value("${public.api.key}")
    private String serviceKey;

    @Value("${public.api.url}")
    private String apiUrl;

    @Autowired
    public PublicDataService(RestClient restClient, PublicDataDao publicDataDao) {
        this.restClient = restClient;
        this.publicDataDao = publicDataDao;
    }

    public DonationProgramDto.ApiResponse fetchDonationPrograms(int pageNo, int numOfRows) {
        String urlString = String.format("%s?serviceKey=%s&pageNo=%d&numOfRows=%d",
                apiUrl, serviceKey, pageNo, numOfRows);

        DonationProgramDto.ApiResponse response = restClient.get()
                .uri(URI.create(urlString))
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(DonationProgramDto.ApiResponse.class);

        return response != null ? response : new DonationProgramDto.ApiResponse();
    }

    /**
     * 공공데이터 수집 건들을 벌크로 저장/갱신하는 비즈니스 메서드
     */
    @Transactional
    public void upsertCampaigns(List<DonationProgramDto> campaigns) {
        if (campaigns == null || campaigns.isEmpty()) {
            return;
        }
        publicDataDao.upsertCampaignBatch(campaigns);
    }

    /**
     * 전체 리로드 초기화: 동기화 전에 기부 내역 + 캠페인을 모두 비운다.
     * ⚠️ 기부 기록까지 삭제되어 되돌릴 수 없다. (FK 순서상 donation → campaign 순)
     */
    @Transactional
    public void clearAllForReload() {
        publicDataDao.deleteAllDonations();
        publicDataDao.deleteAllCampaigns();
    }
}

