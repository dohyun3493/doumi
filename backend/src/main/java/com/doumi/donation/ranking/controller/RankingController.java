package com.doumi.donation.ranking.controller;

import com.doumi.donation.ranking.model.dto.RankingResponse;
import com.doumi.donation.ranking.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    @Autowired
    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ResponseEntity<?> getTopRankings(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "10") int limit) {
        List<RankingResponse> rankings = rankingService.getTopRankings(yearMonth, limit);
        return ResponseEntity.ok(rankings);
    }
}
