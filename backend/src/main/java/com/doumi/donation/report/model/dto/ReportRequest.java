package com.doumi.donation.report.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 사용 보고 작성 요청 */
@Data
public class ReportRequest {

    @NotBlank(message = "보고 내용을 입력해 주세요.")
    private String content;

    @NotEmpty(message = "지출 항목을 1개 이상 입력해 주세요.")
    @Valid
    private List<ExpenseItem> expenses;

    /** 첨부 사진 (영수증 / 후기). 선택 사항이라 비어 있어도 됨. */
    @Valid
    private List<ImageItem> images = new ArrayList<>();

    @Data
    public static class ExpenseItem {
        @NotBlank(message = "지출 항목명을 입력해 주세요.")
        private String item;

        @Min(value = 1, message = "지출 금액은 1원 이상이어야 합니다.")
        private long amount;
    }

    @Data
    public static class ImageItem {
        @NotBlank(message = "이미지 경로가 비어 있습니다.")
        private String url;

        @Pattern(regexp = "RECEIPT|REVIEW", message = "이미지 종류는 RECEIPT 또는 REVIEW만 가능합니다.")
        private String type;
    }
}
