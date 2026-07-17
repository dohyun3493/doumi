package com.doumi.donation.member.controller;

import com.doumi.donation.config.jwt.JWTUtil;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.oauth.KakaoClient;
import com.doumi.donation.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JWTUtil jwtUtil;
    private final MemberService memberService;
    private final KakaoClient kakaoClient;

    public AuthController(JWTUtil jwtUtil, MemberService memberService, KakaoClient kakaoClient) {
        this.jwtUtil = jwtUtil;
        this.memberService = memberService;
        this.kakaoClient = kakaoClient;
    }

    /**
     * 카카오 로그인: 프론트가 받은 인가코드(code)와 redirectUri를 받아
     * 카카오 사용자 확인 → 회원 find-or-create → 우리 서비스 JWT를 발급한다.
     * 응답 형식은 일반 로그인과 동일하다. ({ accessToken, refreshToken })
     */
    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String redirectUri = body.get("redirectUri");
        if (code == null || code.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "code와 redirectUri가 필요합니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }

        try {
            // 1. 인가코드 → 카카오 액세스토큰 → 사용자 정보
            String kakaoToken = kakaoClient.getAccessToken(code, redirectUri);
            KakaoClient.KakaoUser kakaoUser = kakaoClient.getUser(kakaoToken);

            // 2. 회원 find-or-create
            Member member = memberService.findOrCreateKakaoMember(kakaoUser.id(), kakaoUser.nickname());

            // 3. 우리 서비스 JWT 발급 + Refresh 저장 (일반 로그인과 동일)
            String accessToken = jwtUtil.createAccessToken(member);
            String refreshToken = jwtUtil.createRefreshToken(member);
            memberService.updateRefreshToken(member.getEmail(), refreshToken);

            Map<String, String> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);
            Map<String, String> err = new HashMap<>();
            err.put("error", "카카오 로그인에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
    }

    // 리프레시 토큰으로 Access/Refresh 토큰 갱신 API
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        
        // 1. 헤더에 리프레시 토큰이 비어 있는지 확인
        if (refreshToken == null || refreshToken.isEmpty()) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Refresh token is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }

        try {
            // 2. 리프레시 토큰 해독 및 이메일 획득
            Claims claims = jwtUtil.getClaims(refreshToken);
            String email = (String) claims.get("email");
            
            if (email == null) {
                throw new JwtException("Invalid refresh token: email claim missing");
            }

            // 3. DB에서 유저 조회 (우리 서비스의 이메일 조회 메소드 사용)
            Member selected = memberService.findByEmail(email);

            // 4. DB 검증: 유저가 없거나, DB에 저장된 토큰과 다른 경우 차단
            if (selected == null || selected.getRefresh() == null || !selected.getRefresh().equals(refreshToken)) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("error", "Refresh token is required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMap);
            }

            // 5. 신규 토큰 동시 발급 (Access Token & Refresh Token)
            String newAccessToken = jwtUtil.createAccessToken(selected);
            String newRefreshToken = jwtUtil.createRefreshToken(selected);

            // 6. 새로 발급받은 Refresh Token을 다시 DB에 업데이트 (RTR 기법)
            selected.setRefresh(newRefreshToken);
            memberService.updateRefreshToken(selected.getEmail(), newRefreshToken);

            // 7. 성공 결과 전송
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("accessToken", newAccessToken);
            responseMap.put("refreshToken", newRefreshToken);

            return ResponseEntity.status(HttpStatus.OK).body(responseMap);

        } catch (Exception e) {
            // 토큰 해독 실패 또는 만료인 경우 401 Unauthorized 에러 리턴
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Invalid or expired refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMap);
        }
    }
}
