package com.doumi.donation.ranking.model.dto;

import lombok.Data;

@Data
public class RankingResponse {
    private Integer rank;
    private String memberName;
    private Long totalAmount;
}
