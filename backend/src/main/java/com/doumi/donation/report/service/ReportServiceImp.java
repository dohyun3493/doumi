package com.doumi.donation.report.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.service.CampaignsService;
import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import com.fasterxml.jackson.databind.JsonNode;
import com.doumi.donation.file.service.FileStorageService;
import com.doumi.donation.notification.model.dao.NotificationDao;
import com.doumi.donation.report.ai.GeminiClient;
import com.doumi.donation.report.model.dao.ReportDao;
import com.doumi.donation.report.model.dto.CampaignReport;
import com.doumi.donation.report.model.dto.ReportDraftRequest;
import com.doumi.donation.report.model.dto.ReportDraftResponse;
import com.doumi.donation.report.model.dto.ReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImp implements ReportService {

    private final ReportDao reportDao;
    private final CampaignsDao campaignsDao;
    private final CampaignsService campaignsService;
    private final MemberDao memberDao;
    private final NotificationDao notificationDao;
    private final GeminiClient geminiClient;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void createReport(long memberId, long campaignId, ReportRequest req) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }

        // 작성 권한: 캠페인 소유 단체 본인 또는 관리자
        Member writer = memberDao.findByMemberId(memberId);
        boolean isAdmin = writer != null && "ADMIN".equals(writer.getMemberType());
        boolean isOwner = campaign.getOwnerId() != null && campaign.getOwnerId() == memberId;
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("캠페인을 등록한 단체만 사용 보고를 작성할 수 있습니다.");
        }

        // 도메인 규칙: 모금이 종료(모집완료)되었거나 전달완료된 캠페인만 보고 가능, 보고는 캠페인당 1회
        if (!"모집완료".equals(campaign.getStatus()) && !"전달완료".equals(campaign.getStatus())) {
            throw new IllegalArgumentException("모집완료 또는 전달완료된 캠페인만 사용 보고를 작성할 수 있습니다.");
        }
        if (reportDao.existsByCampaignId(campaignId)) {
            throw new DuplicateResourceException("이미 사용 보고가 등록된 캠페인입니다.");
        }

        // 지출 합계는 모금액을 초과할 수 없음
        long totalSpent = 0;

        for (ReportRequest.ExpenseItem item : req.getExpenses()) {
            totalSpent += item.getAmount();
        }

        long raised = campaign.getCurrentAmount() != null ? campaign.getCurrentAmount() : 0;
        if (totalSpent > raised) {
            throw new IllegalArgumentException(
                    "지출 합계(" + totalSpent + "원)가 모금액(" + raised + "원)을 초과할 수 없습니다.");
        }

        // 보고서 + 지출 항목 저장
        CampaignReport report = new CampaignReport();
        report.setCampaignId(campaignId);
        report.setContent(req.getContent());
        reportDao.insertReport(report);
        for (ReportRequest.ExpenseItem expense : req.getExpenses()) {
            reportDao.insertExpense(report.getReportId(), expense.getItem(), expense.getAmount());
        }

        // 첨부 사진(영수증 / 후기) 저장 — 선택 사항
        if (req.getImages() != null) {
            for (ReportRequest.ImageItem image : req.getImages()) {
                reportDao.insertImage(report.getReportId(), image.getUrl(), image.getType());
            }
        }

        // 도메인 규칙: 보고서가 등록돼야 캠페인이 '사용완료'로 종결됨 (벡터 DB 동기화 포함)
        // 위에서 이미 소유 단체/관리자임을 검증했으므로 같은 memberId로 객체 레벨 인가를 통과
        Campaign statusUpdate = new Campaign();
        statusUpdate.setStatus("사용완료");
        campaignsService.updateCampaign(campaignId, memberId, statusUpdate);

        // 이 캠페인의 기부자 전원에게 알림 (실패해도 보고 등록은 유지)
        try {
            notificationDao.insertReportNotifications(campaignId);
        } catch (Exception e) {
            log.error("사용 보고 알림 생성 실패: campaignId={}", campaignId, e);
        }
    }

    // 이미 등록된 사용 보고 수정. 본문/지출/이미지를 교체한다.
    // (status는 이미 '사용완료'이므로 변경하지 않고, 알림도 다시 보내지 않는다)
    @Override
    @Transactional
    public void updateReport(long memberId, long campaignId, ReportRequest req) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }

        // 작성 권한과 동일: 캠페인 소유 단체 본인 또는 관리자
        Member writer = memberDao.findByMemberId(memberId);
        boolean isAdmin = writer != null && "ADMIN".equals(writer.getMemberType());
        boolean isOwner = campaign.getOwnerId() != null && campaign.getOwnerId() == memberId;
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("캠페인을 등록한 단체만 사용 보고를 수정할 수 있습니다.");
        }

        CampaignReport report = reportDao.findReportByCampaignId(campaignId);
        if (report == null) {
            throw new ResourceNotFoundException("수정할 사용 보고가 없습니다.");
        }

        // 지출 합계는 모금액을 초과할 수 없음 (등록과 동일 규칙)
        long totalSpent = 0;
        for (ReportRequest.ExpenseItem item : req.getExpenses()) {
            totalSpent += item.getAmount();
        }
        long raised = campaign.getCurrentAmount() != null ? campaign.getCurrentAmount() : 0;
        if (totalSpent > raised) {
            throw new IllegalArgumentException(
                    "지출 합계(" + totalSpent + "원)가 모금액(" + raised + "원)을 초과할 수 없습니다.");
        }

        long reportId = report.getReportId();

        // 본문 갱신
        reportDao.updateReportContent(reportId, req.getContent());

        // 지출 항목 교체 (전부 삭제 후 재삽입)
        reportDao.deleteExpensesByReportId(reportId);
        for (ReportRequest.ExpenseItem expense : req.getExpenses()) {
            reportDao.insertExpense(reportId, expense.getItem(), expense.getAmount());
        }

        // 첨부 사진 교체 (전부 삭제 후 재삽입) — 선택 사항
        reportDao.deleteImagesByReportId(reportId);
        if (req.getImages() != null) {
            for (ReportRequest.ImageItem image : req.getImages()) {
                reportDao.insertImage(reportId, image.getUrl(), image.getType());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDraftResponse generateDraft(long memberId, long campaignId, ReportDraftRequest req) {
        Campaign campaign = campaignsDao.getDetailCampaign(campaignId);
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }

        // 작성 권한과 동일하게: 소유 단체 본인 또는 관리자만 초안 생성 가능
        Member writer = memberDao.findByMemberId(memberId);
        boolean isAdmin = writer != null && "ADMIN".equals(writer.getMemberType());
        boolean isOwner = campaign.getOwnerId() != null && campaign.getOwnerId() == memberId;
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("캠페인을 등록한 단체만 사용 보고 초안을 생성할 수 있습니다.");
        }

        // 첨부 이미지(영수증/사진)를 멀티모달 입력으로 변환
        List<GeminiClient.InlineImage> images = new ArrayList<>();
        if (req.getImageUrls() != null) {
            for (String url : req.getImageUrls()) {
                if (url == null || url.isBlank()) continue;
                try {
                    FileStorageService.StoredImage img = fileStorageService.load(url);
                    images.add(new GeminiClient.InlineImage(img.mimeType(), img.bytes()));
                } catch (Exception e) {
                    log.warn("AI 초안용 이미지 로드 실패: {}", url, e);
                }
            }
        }

        JsonNode result = geminiClient.generateJson(buildDraftPrompt(campaign, req), images, draftSchema());
        return mapToDraft(result);
    }

    /** AI에게 강제할 JSON 출력 스키마: { content, expenses:[{item, amount}] } */
    private Map<String, Object> draftSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "content", Map.of("type", "STRING"),
                        "expenses", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "item", Map.of("type", "STRING"),
                                                "amount", Map.of("type", "INTEGER"))))),
                "required", List.of("content", "expenses"));
    }

    private ReportDraftResponse mapToDraft(JsonNode node) {
        String content = node.path("content").asText("").trim();
        List<ReportDraftResponse.ExpenseItem> expenses = new ArrayList<>();
        for (JsonNode e : node.path("expenses")) {
            String item = e.path("item").asText("").trim();
            long amount = e.path("amount").asLong(0);
            if (!item.isBlank() && amount > 0) {
                expenses.add(new ReportDraftResponse.ExpenseItem(item, amount));
            }
        }
        return new ReportDraftResponse(content, expenses);
    }

    /** 첨부 이미지·메모·지출 힌트로 보고 본문 + 지출 내역을 정리하도록 지시하는 프롬프트. */
    private String buildDraftPrompt(Campaign campaign, ReportDraftRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 기부 단체의 '기부금 사용 보고서' 작성을 돕는 도우미입니다.\n");
        sb.append("첨부된 영수증/사진 이미지와 아래 정보를 보고, 기부자에게 전할 ");
        sb.append("'보고 본문(content)'과 '지출 내역(expenses)'을 JSON으로 정리하세요.\n\n");
        sb.append("[작성 규칙]\n");
        sb.append("- content: 따뜻하고 진정성 있는 어조의 한국어 3~5문장. 반드시 기부자에 대한 감사 인사 포함.\n");
        sb.append("- expenses: 영수증/메모에서 확인되는 지출 항목과 금액(원, 정수)만 추출. 항목명은 한국어로.\n");
        sb.append("- 이미지·메모에서 확인되지 않는 금액이나 내용을 임의로 지어내지 말 것.\n");
        sb.append("- 지출 항목을 하나도 확인할 수 없으면 expenses는 빈 배열로 둘 것.\n");
        sb.append("- content에는 제목·머리말·마크다운 기호 없이 본문 텍스트만.\n\n");
        sb.append("[캠페인 정보]\n");
        if (campaign.getOrgName() != null && !campaign.getOrgName().isBlank()) {
            sb.append("- 단체: ").append(campaign.getOrgName()).append("\n");
        }
        sb.append("- 캠페인: ").append(campaign.getTitle()).append("\n");
        if (campaign.getPurpose() != null && !campaign.getPurpose().isBlank()) {
            sb.append("- 모집 목적: ").append(campaign.getPurpose()).append("\n");
        }
        long raised = campaign.getCurrentAmount() != null ? campaign.getCurrentAmount() : 0;
        sb.append("- 총 모금액: ").append(raised).append("원 (지출 합계는 이 금액을 넘지 않아야 함)\n");

        if (req.getNote() != null && !req.getNote().isBlank()) {
            sb.append("\n[단체가 남긴 메모]\n").append(req.getNote()).append("\n");
        }

        boolean hasHint = req.getExpenses() != null && req.getExpenses().stream()
                .anyMatch(e -> e.getItem() != null && !e.getItem().isBlank());
        if (hasHint) {
            sb.append("\n[단체가 미리 적어둔 지출 힌트 (참고용)]\n");
            for (ReportDraftRequest.ExpenseItem e : req.getExpenses()) {
                if (e.getItem() == null || e.getItem().isBlank()) continue;
                sb.append("- ").append(e.getItem()).append(": ").append(e.getAmount()).append("원\n");
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignReport getReport(long campaignId) {
        CampaignReport report = reportDao.findReportByCampaignId(campaignId);
        if (report == null) {
            throw new ResourceNotFoundException("등록된 사용 보고가 없습니다.");
        }
        report.setExpenses(reportDao.findExpensesByReportId(report.getReportId()));
        report.setImages(reportDao.findImagesByReportId(report.getReportId()));
        long totalSpent = 0;

        for (var e : report.getExpenses()) {
            totalSpent += e.getAmount();
        }

        report.setTotalSpent(totalSpent);

        return report;
    }
}
