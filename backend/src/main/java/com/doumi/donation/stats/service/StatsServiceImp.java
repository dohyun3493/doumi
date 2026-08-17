package com.doumi.donation.stats.service;

import com.doumi.donation.config.CacheConfig;
import com.doumi.donation.stats.model.dao.StatsDao;
import com.doumi.donation.stats.model.dto.StatsResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsServiceImp implements StatsService {

    private final StatsDao statsDao;

    public StatsServiceImp(StatsDao statsDao) {
        this.statsDao = statsDao;
    }

    // 통계 대시보드는 요청마다 집계 쿼리 11개를 수행하므로 Redis에 캐싱 (TTL 5분)
    @Override
    @Cacheable(cacheNames = CacheConfig.STATS_CACHE, key = "'summary'")
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        StatsResponse res = new StatsResponse();
        res.setTotalDonationAmount(statsDao.getTotalDonationAmount());
        res.setActiveCampaigns(statsDao.getActiveCampaignCount());
        res.setTotalMembers(statsDao.getTotalMemberCount());
        res.setMonthlyDonors(statsDao.getMonthlyDonorCount());
        res.setCompletedCampaigns(statsDao.getCompletedCampaignCount());

        res.setIndividualMembers(statsDao.getIndividualMemberCount());
        res.setOrganizationMembers(statsDao.getOrganizationMemberCount());
        res.setPendingOrganizations(statsDao.getPendingOrganizationCount());

        res.setMonthlyDonations(statsDao.getMonthlyDonations());
        res.setCategoryCounts(statsDao.getCategoryCounts());
        res.setRegionCounts(statsDao.getRegionCounts());
        return res;
    }
}
