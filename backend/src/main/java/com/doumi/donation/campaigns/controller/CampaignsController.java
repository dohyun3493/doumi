package com.doumi.donation.campaigns.controller;

import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.model.dto.DeleteRequest;
import com.doumi.donation.campaigns.service.CampaignsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignsController {

    private final CampaignsService campaignsService;

    public CampaignsController(CampaignsService campaignsService){
        this.campaignsService = campaignsService;
    }

    @GetMapping
    public ResponseEntity<?> getCampaigns(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<Campaign> result = campaignsService.getAllCampaigns(category, region, status, keyword);

        // 검색 결과가 없는 것은 에러(404)가 아닌 빈 데이터 상태이므로, 
        // 200 OK와 함께 빈 배열([])을 리턴해 주는 것이 표준 API 설계 사양입니다.
        if (result == null) {
            return ResponseEntity.status(HttpStatus.OK).body(new ArrayList<>());
        }

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /** 마이페이지: 로그인한 단체가 등록한 캠페인 목록 ('/{id}'보다 먼저 매칭됨) */
    @GetMapping("/mine")
    public ResponseEntity<?> getMyCampaigns(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(campaignsService.getMyCampaigns(memberId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetailCampaign(@PathVariable long id){
        Campaign result = campaignsService.getDetailCampaign(id);

        if(result != null){
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping
    public ResponseEntity<?> registCampaign(@AuthenticationPrincipal Long memberId,
                                            @RequestBody Campaign req){
        req.setOwnerId(memberId);   // 등록자(단체)를 캠페인 소유자로 기록
        campaignsService.registCampaign(req);
        return ResponseEntity.status(HttpStatus.CREATED).body("캠페인이 등록되었습니다.");
    }

    @PatchMapping("/{campaignId}")
    public ResponseEntity<?> updateCampaign(@AuthenticationPrincipal Long memberId,
                                            @PathVariable long campaignId,
                                            @RequestBody Campaign req) {
        campaignsService.updateCampaign(campaignId, memberId, req);
        return ResponseEntity.ok("캠페인이 수정되었습니다.");
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<?> deleteCampaign(@AuthenticationPrincipal Long memberId,
                                            @PathVariable long campaignId){
        campaignsService.deleteCampaing(campaignId, memberId);
        return ResponseEntity.ok("캠페인이 삭제되었습니다.");
    }

    @GetMapping("/{id}/donations")
    public ResponseEntity<?> getDonations(@PathVariable long id) {
        return ResponseEntity.ok(campaignsService.getDonationsByCampaignId(id));
    }

    // 단체(소유자)가 캠페인 삭제를 요청 → 관리자 승인/거부 대기
    @PostMapping("/{campaignId}/delete-request")
    public ResponseEntity<?> requestDeletion(@AuthenticationPrincipal Long memberId,
                                             @PathVariable long campaignId,
                                             @RequestBody(required = false) DeleteRequest req) {
        String reason = (req != null) ? req.getReason() : null;
        campaignsService.requestDeletion(campaignId, memberId, reason);
        return ResponseEntity.ok("삭제 요청이 접수되었습니다. 관리자 승인 후 삭제됩니다.");
    }
}
