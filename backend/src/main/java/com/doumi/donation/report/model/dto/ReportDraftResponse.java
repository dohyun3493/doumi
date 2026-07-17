package com.doumi.donation.report.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 사용 보고 AI 초안 결과 — 보고 본문 + 정리된 지출 내역. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDraftResponse {

    private String content;
    private List<ExpenseItem> expenses = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseItem {
        private String item;
        private long amount;
    }
}
