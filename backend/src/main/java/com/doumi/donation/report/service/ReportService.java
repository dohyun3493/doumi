package com.doumi.donation.report.service;

import com.doumi.donation.report.model.dto.CampaignReport;
import com.doumi.donation.report.model.dto.ReportDraftRequest;
import com.doumi.donation.report.model.dto.ReportDraftResponse;
import com.doumi.donation.report.model.dto.ReportRequest;

public interface ReportService {
    void createReport(long memberId, long campaignId, ReportRequest req);
    void updateReport(long memberId, long campaignId, ReportRequest req);
    CampaignReport getReport(long campaignId);

    /**
     * 사용 보고 AI 초안 생성 (소유 단체/관리자만).
     * 첨부 이미지(영수증 등) + 메모를 바탕으로 보고 본문과 지출 내역을 정리해 반환한다. 저장은 하지 않음.
     */
    ReportDraftResponse generateDraft(long memberId, long campaignId, ReportDraftRequest req);
}
