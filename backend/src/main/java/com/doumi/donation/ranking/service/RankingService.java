package com.doumi.donation.ranking.service;

import com.doumi.donation.ranking.model.dto.RankingResponse;
import java.util.List;

public interface RankingService {
    List<RankingResponse> getTopRankings(String yearMonth, int limit);
}
