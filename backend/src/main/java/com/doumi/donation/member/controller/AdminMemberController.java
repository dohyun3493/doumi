package com.doumi.donation.member.controller;

import com.doumi.donation.member.service.MemberService;
import com.doumi.donation.notification.model.dto.NotifyRequest;
import com.doumi.donation.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 관리자 회원 관리 (조회 / 강제 탈퇴 / 직접 알림). /api/admin/** 은 시큐리티에서 ADMIN 강제 */
@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;
    private final NotificationService notificationService;

    public AdminMemberController(MemberService memberService, NotificationService notificationService) {
        this.memberService = memberService;
        this.notificationService = notificationService;
    }

    /** 회원 목록 (유형/검색어 필터 + 페이징, 기본 20개) */
    @GetMapping
    public ResponseEntity<?> getMembers(@RequestParam(required = false) String type,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(memberService.getAllMembers(type, keyword, page, size));
    }

    /** 회원 강제 탈퇴 (soft delete) */
    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> forceWithdraw(@PathVariable long memberId) {
        memberService.forceWithdraw(memberId);
        return ResponseEntity.ok("회원을 강제 탈퇴 처리했습니다.");
    }

    /** 회원에게 직접 알림 전송 */
    @PostMapping("/{memberId}/notify")
    public ResponseEntity<?> notify(@PathVariable long memberId,
                                    @Valid @RequestBody NotifyRequest req) {
        memberService.findByMemberId(memberId); // 존재 검증 (없으면 404)
        notificationService.sendToMember(memberId, req.getContent(), req.getLinkUrl());
        return ResponseEntity.ok("알림을 전송했습니다.");
    }
}
