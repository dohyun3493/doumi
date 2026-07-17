package com.doumi.donation.member.controller;

import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.member.service.EmailVerificationService;
import com.doumi.donation.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 이메일 인증코드 발송/검증 및 비밀번호 재설정 API.
 * SecurityConfig에서 /api/v1/auth/** 는 공개로 열려 있다.
 *
 * <ul>
 *   <li>POST /email/send-code   회원가입용 인증코드 발송 (미가입 이메일만)</li>
 *   <li>POST /email/verify-code 인증코드 검증 → 가입 가능 상태로 표시</li>
 *   <li>POST /password/send-code 비밀번호 재설정 코드 발송 (가입된 이메일만)</li>
 *   <li>POST /password/reset     코드 검증 후 새 비밀번호로 변경</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class EmailAuthController {

    private final EmailVerificationService emailVerificationService;
    private final MemberService memberService;

    public EmailAuthController(EmailVerificationService emailVerificationService,
                              MemberService memberService) {
        this.emailVerificationService = emailVerificationService;
        this.memberService = memberService;
    }

    // ===== 회원가입 이메일 인증 =====

    @PostMapping("/email/send-code")
    public ResponseEntity<?> sendSignupCode(@RequestBody Map<String, String> body) {
        String email = requireEmail(body);
        if (memberService.existsByEmail(email)) {
            throw new DuplicateResourceException("이미 가입된 이메일입니다.");
        }
        emailVerificationService.sendSignupCode(email);
        return ResponseEntity.ok(Map.of("message", "인증코드를 메일로 보냈습니다."));
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<?> verifySignupCode(@RequestBody Map<String, String> body) {
        String email = requireEmail(body);
        String code = requireCode(body);
        emailVerificationService.verifySignupCode(email, code);
        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }

    // ===== 비밀번호 재설정 =====

    @PostMapping("/password/send-code")
    public ResponseEntity<?> sendResetCode(@RequestBody Map<String, String> body) {
        String email = requireEmail(body);
        if (!memberService.existsByEmail(email)) {
            throw new ResourceNotFoundException("가입되지 않은 이메일입니다.");
        }
        // 카카오 전용 계정은 비밀번호 로그인을 쓰지 않으므로 재설정 대상에서 제외
        if (email.endsWith("@kakao.local")) {
            throw new IllegalArgumentException("카카오로 가입한 계정은 비밀번호를 사용하지 않습니다. 카카오 로그인을 이용해 주세요.");
        }
        emailVerificationService.sendResetCode(email);
        return ResponseEntity.ok(Map.of("message", "인증코드를 메일로 보냈습니다."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = requireEmail(body);
        String code = requireCode(body);
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해 주세요.");
        }
        // 코드 검증을 먼저 통과해야 비밀번호를 변경한다 (검증 실패 시 예외로 중단)
        emailVerificationService.verifyResetCode(email, code);
        memberService.resetPassword(email, newPassword);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요."));
    }

    // ===== 입력 검증 =====

    private String requireEmail(Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        return email.trim();
    }

    private String requireCode(Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인증코드를 입력해 주세요.");
        }
        return code.trim();
    }
}
