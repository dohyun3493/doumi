package com.doumi.donation.campaigns.controller;

import com.doumi.donation.campaigns.model.dto.DeleteRequest;
import com.doumi.donation.campaigns.service.CampaignsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 관리자 캠페인 관리. /api/admin/** 은 시큐리티에서 ADMIN 권한 강제 */
@RestController
@RequestMapping("/api/admin/campaigns")
public class AdminCampaignController {

    private final CampaignsService campaignsService;

    public AdminCampaignController(CampaignsService campaignsService) {
        this.campaignsService = campaignsService;
    }

    /** 기부금 전달 처리 → status '전달완료' */
    @PostMapping("/{campaignId}/deliver")
    public ResponseEntity<?> deliver(@PathVariable long campaignId) {
        campaignsService.deliverCampaign(campaignId);
        return ResponseEntity.ok("기부금 전달 처리가 완료되었습니다.");
    }

    /** 삭제 요청된 캠페인 목록 */
    @GetMapping("/delete-requests")
    public ResponseEntity<?> getDeleteRequests() {
        return ResponseEntity.ok(campaignsService.getDeleteRequestedCampaigns());
    }

    /** 삭제 요청 승인 (실제 삭제 + 소유자 알림) */
    @PostMapping("/{campaignId}/delete-approve")
    public ResponseEntity<?> approveDeletion(@PathVariable long campaignId) {
        campaignsService.approveDeletion(campaignId);
        return ResponseEntity.ok("캠페인 삭제 요청을 승인했습니다.");
    }

    /** 삭제 요청 거부 (요청 해제 + 소유자 알림) */
    @PostMapping("/{campaignId}/delete-reject")
    public ResponseEntity<?> rejectDeletion(@PathVariable long campaignId,
                                            @RequestBody(required = false) DeleteRequest req) {
        String reason = (req != null) ? req.getReason() : null;
        campaignsService.rejectDeletion(campaignId, reason);
        return ResponseEntity.ok("캠페인 삭제 요청을 거부했습니다.");
    }
}
