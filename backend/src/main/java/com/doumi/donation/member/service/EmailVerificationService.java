package com.doumi.donation.member.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

import java.security.SecureRandom;

/**
 * 이메일 인증코드 / 비밀번호 재설정 코드를 Redis에 TTL과 함께 저장하고 검증한다.
 * 코드 저장소로 챗봇과 공유하는 JedisPooled를 재사용한다.
 *
 * <p>회원/비밀번호 도메인 로직(존재 확인, 비번 변경)은 호출하는 컨트롤러/서비스가 담당하고,
 * 이 클래스는 "코드 발급·검증·인증완료 표시"만 책임진다. (MemberService와 순환참조 방지)
 */
@Slf4j
@Service
public class EmailVerificationService {

    // 6자리 숫자 코드, 발송 주기 제한(재발송 쿨다운)
    private static final int CODE_LENGTH = 6;
    private static final long SIGNUP_CODE_TTL = 5 * 60;        // 5분
    private static final long SIGNUP_VERIFIED_TTL = 30 * 60;   // 인증 후 가입 완료까지 30분
    private static final long RESET_CODE_TTL = 30 * 60;        // 30분
    private static final long RESEND_COOLDOWN = 60;            // 재발송 최소 간격 60초

    private final JedisPooled redis;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(JedisPooled redis, MailService mailService) {
        this.redis = redis;
        this.mailService = mailService;
    }

    // ===== 회원가입 이메일 인증 =====

    public void sendSignupCode(String email) {
        enforceCooldown(cooldownKey(email));
        String code = generateCode();
        redis.setex(signupCodeKey(email), SIGNUP_CODE_TTL, code);
        mailService.sendSignupCode(email, code);
    }

    /** 코드 일치 시 인증완료 플래그를 남기고 코드는 폐기한다. 불일치/만료면 예외. */
    public void verifySignupCode(String email, String code) {
        String saved = redis.get(signupCodeKey(email));
        if (saved == null) {
            throw new IllegalArgumentException("인증코드가 만료되었습니다. 코드를 다시 요청해 주세요.");
        }
        if (!saved.equals(code)) {
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }
        redis.del(signupCodeKey(email));
        redis.setex(signupVerifiedKey(email), SIGNUP_VERIFIED_TTL, "1");
    }

    public boolean isSignupVerified(String email) {
        return redis.exists(signupVerifiedKey(email));
    }

    /** 가입 완료 후 인증 플래그 정리 */
    public void clearSignupVerified(String email) {
        redis.del(signupVerifiedKey(email));
    }

    // ===== 비밀번호 재설정 =====

    public void sendResetCode(String email) {
        enforceCooldown(cooldownKey(email));
        String code = generateCode();
        redis.setex(resetCodeKey(email), RESET_CODE_TTL, code);
        mailService.sendPasswordResetCode(email, code);
    }

    /** 코드 일치 시 코드를 폐기한다. 불일치/만료면 예외. (비번 변경은 호출 측 책임) */
    public void verifyResetCode(String email, String code) {
        String saved = redis.get(resetCodeKey(email));
        if (saved == null) {
            throw new IllegalArgumentException("인증코드가 만료되었습니다. 코드를 다시 요청해 주세요.");
        }
        if (!saved.equals(code)) {
            throw new IllegalArgumentException("인증코드가 일치하지 않습니다.");
        }
        redis.del(resetCodeKey(email));
    }

    // ===== 내부 유틸 =====

    /** 같은 이메일로 60초 내 재발송을 막는다. */
    private void enforceCooldown(String key) {
        // SET key 1 NX EX 60 — 키가 없을 때만 설정. 이미 있으면 쿨다운 중.
        String result = redis.set(key, "1", new SetParams().nx().ex(RESEND_COOLDOWN));
        if (result == null) {
            throw new IllegalArgumentException("인증코드는 1분에 한 번만 보낼 수 있습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String signupCodeKey(String email)     { return "auth:signup:code:" + email; }
    private String signupVerifiedKey(String email) { return "auth:signup:verified:" + email; }
    private String resetCodeKey(String email)      { return "auth:reset:code:" + email; }
    private String cooldownKey(String email)       { return "auth:mail:cooldown:" + email; }
}
