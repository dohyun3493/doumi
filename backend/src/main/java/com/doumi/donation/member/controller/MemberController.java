package com.doumi.donation.member.controller;

import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.member.model.dto.MemberJoinRequest;
import com.doumi.donation.member.model.dto.MemberUpdateRequest;
import com.doumi.donation.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 회원가입 (공개)
    @PostMapping
    public ResponseEntity<?> join(@RequestBody @Valid MemberJoinRequest req) {
        memberService.join(req);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    // 회원 정보 조회 (본인만)
    @GetMapping("/{email:.+}")
    public ResponseEntity<?> getMember(@AuthenticationPrincipal Long memberId,
                                       @PathVariable String email) {
        verifyOwner(memberId, email, "본인 정보만 조회할 수 있습니다.");
        return ResponseEntity.ok(memberService.findByEmail(email));
    }

    // 회원별 기부 내역 조회 (본인만)
    @GetMapping("/{email:.+}/donations")
    public ResponseEntity<?> getDonations(@AuthenticationPrincipal Long memberId,
                                          @PathVariable String email) {
        verifyOwner(memberId, email, "본인 기부 내역만 조회할 수 있습니다.");
        return ResponseEntity.ok(memberService.findDonationsByEmail(email));
    }

    // 회원 탈퇴 (본인만)
    @DeleteMapping("/{email:.+}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal Long memberId,
                                    @PathVariable String email) {
        verifyOwner(memberId, email, "본인만 탈퇴할 수 있습니다.");
        memberService.delete(email);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }

    // 회원 정보 수정 (본인만)
    @PatchMapping("/{email:.+}")
    public ResponseEntity<?> update(@AuthenticationPrincipal Long memberId,
                                    @PathVariable String email,
                                    @RequestBody @Valid MemberUpdateRequest req) {
        verifyOwner(memberId, email, "본인 정보만 수정할 수 있습니다.");
        memberService.update(email, req);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    // 토큰의 memberId가 path의 email 주인과 다르면 403 (GlobalExceptionHandler가 처리)
    private void verifyOwner(Long memberId, String email, String message) {
        if (memberId == null
                || !memberService.findByMemberId(memberId).getEmail().equals(email)) {
            throw new UnauthorizedException(message);
        }
    }
}
