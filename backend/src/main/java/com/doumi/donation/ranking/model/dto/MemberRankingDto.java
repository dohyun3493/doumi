package com.doumi.donation.ranking.model.dto;

import lombok.Data;

@Data
public class MemberRankingDto {
    private Long memberId;
    private String yearMonth;
    private Long totalAmount;
    private Integer rank;
}
