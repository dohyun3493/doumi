package com.doumi.donation.report.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용 보고 AI 초안 생성 요청 (멀티모달).
 * 단체가 대충 넘긴 메모/이미지(영수증·사진)/지출 힌트를 받아,
 * AI가 보고 내용 + 지출 내역을 정리하도록 한다. 초안 단계라 엄격한 검증은 두지 않는다.
 */
@Data
public class ReportDraftRequest {

    /** 자유 서술 메모 (예: "영수증 보고 정리해줘, 연탄 200가구 전달함") */
    private String note;

    /** 첨부 이미지 경로(/uploads/...). 영수증·활동 사진 등 — AI가 읽어 참고. */
    private List<String> imageUrls = new ArrayList<>();

    /** 단체가 미리 적어둔 지출 힌트(선택). 있으면 참고용으로 함께 전달. */
    private List<ExpenseItem> expenses = new ArrayList<>();

    @Data
    public static class ExpenseItem {
        private String item;
        private long amount;
    }
}
