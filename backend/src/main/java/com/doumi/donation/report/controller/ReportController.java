package com.doumi.donation.report.controller;

import com.doumi.donation.report.model.dto.ReportDraftRequest;
import com.doumi.donation.report.model.dto.ReportRequest;
import com.doumi.donation.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 사용 보고 조회 (공개) — 없으면 404 */
    @GetMapping
    public ResponseEntity<?> getReport(@PathVariable long campaignId) {
        return ResponseEntity.ok(reportService.getReport(campaignId));
    }

    /** 사용 보고 작성 (캠페인 소유 단체 또는 관리자) */
    @PostMapping
    public ResponseEntity<?> createReport(@AuthenticationPrincipal Long memberId,
                                          @PathVariable long campaignId,
                                          @Valid @RequestBody ReportRequest req) {
        reportService.createReport(memberId, campaignId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body("사용 보고가 등록되었습니다.");
    }

    /** 사용 보고 수정 (캠페인 소유 단체 또는 관리자) */
    @PutMapping
    public ResponseEntity<?> updateReport(@AuthenticationPrincipal Long memberId,
                                          @PathVariable long campaignId,
                                          @Valid @RequestBody ReportRequest req) {
        reportService.updateReport(memberId, campaignId, req);
        return ResponseEntity.ok("사용 보고가 수정되었습니다.");
    }

    /** 사용 보고 AI 초안 생성 — 영수증/사진/메모로 보고 본문 + 지출 내역을 정리 (소유 단체/관리자) */
    @PostMapping("/draft")
    public ResponseEntity<?> draftReport(@AuthenticationPrincipal Long memberId,
                                         @PathVariable long campaignId,
                                         @RequestBody ReportDraftRequest req) {
        return ResponseEntity.ok(reportService.generateDraft(memberId, campaignId, req));
    }
}
