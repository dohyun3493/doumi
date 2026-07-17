package com.doumi.donation.stats.model.dto;

import lombok.Data;

/** 라벨별 집계 (카테고리별·지역별 캠페인 수 등) */
@Data
public class LabelCountDto {
    private String label;   // 카테고리명 또는 지역명
    private long count;     // 캠페인 수
}
