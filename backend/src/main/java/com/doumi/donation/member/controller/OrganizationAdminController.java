package com.doumi.donation.member.controller;

import com.doumi.donation.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 단체 회원 승인/거절 (관리자 전용 — /api/admin/** 은 시큐리티에서 ADMIN 권한 강제) */
@RestController
@RequestMapping("/api/admin/organizations")
public class OrganizationAdminController {

    private final MemberService memberService;

    public OrganizationAdminController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> getOrganizations(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(memberService.getOrganizations(status));
    }

    @PatchMapping("/{memberId}/approve")
    public ResponseEntity<?> approve(@PathVariable long memberId) {
        memberService.approveOrganization(memberId);
        return ResponseEntity.ok("단체를 승인했습니다.");
    }

    @PatchMapping("/{memberId}/reject")
    public ResponseEntity<?> reject(@PathVariable long memberId,
                                    @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        memberService.rejectOrganization(memberId, reason);
        return ResponseEntity.ok("단체 승인을 거절했습니다.");
    }
}
