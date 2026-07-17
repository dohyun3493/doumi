package com.doumi.donation.report.model.dto;

import lombok.Data;

/** 사용 보고의 지출 항목 */
@Data
public class ReportExpense {
    private long expenseId;
    private long reportId;
    private String item;
    private long amount;
}
