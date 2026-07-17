package com.doumi.donation.ranking.service;

import com.doumi.donation.ranking.model.dao.RankingDao;
import com.doumi.donation.ranking.model.dto.RankingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RankingServiceImp implements RankingService {

    private final RankingDao rankingDao;

    @Autowired
    public RankingServiceImp(RankingDao rankingDao) {
        this.rankingDao = rankingDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> getTopRankings(String yearMonth, int limit) {
        if (yearMonth == null || !yearMonth.matches("^\\d{4}-\\d{2}$")) {
            throw new IllegalArgumentException("올바른 조회 월 형식(YYYY-MM)이 아닙니다.");
        }
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }
        List<RankingResponse> rankings = rankingDao.selectTopRankings(yearMonth, limit);
        // 개인정보 보호: 기부자 이름 가운데를 * 처리 (김도윤 → 김*윤, 김도 → 김*)
        rankings.forEach(r -> r.setMemberName(maskName(r.getMemberName())));
        return rankings;
    }

    // 이름 가운데 글자를 * 로 마스킹. 2글자는 마지막 글자를, 3글자 이상은 가운데 전부를 가린다.
    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        int len = name.length();
        if (len <= 1) {
            return name;
        }
        if (len == 2) {
            return name.charAt(0) + "*";
        }
        StringBuilder masked = new StringBuilder();
        masked.append(name.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            masked.append('*');
        }
        masked.append(name.charAt(len - 1));
        return masked.toString();
    }
}
