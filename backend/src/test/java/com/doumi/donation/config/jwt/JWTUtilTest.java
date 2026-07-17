package com.doumi.donation.config.jwt;

import com.doumi.donation.member.model.dto.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {

    private static final String SECRET = "testSecretKeyForJwtMustBeAtLeast32CharactersLong!!";

    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JWTUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
        jwtUtil.init();
    }

    private Member member() {
        Member m = new Member();
        m.setMemberId(7L);
        m.setEmail("user@test.com");
        m.setMemberType("USER");
        return m;
    }

    @Test
    @DisplayName("AccessToken 생성 후 파싱하면 이메일/memberId/role 클레임이 그대로 나온다")
    void createAndParseAccessToken() {
        String token = jwtUtil.createAccessToken(member());

        Claims claims = jwtUtil.getClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@test.com");
        assertThat(claims.get("memberId", Long.class)).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("서명이 변조된 토큰은 예외가 발생한다")
    void tamperedTokenIsRejected() {
        String token = jwtUtil.createAccessToken(member());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtUtil.getClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 거부된다")
    void tokenSignedWithDifferentSecretIsRejected() {
        JWTUtil other = new JWTUtil();
        ReflectionTestUtils.setField(other, "secretKey",
                "anotherSecretKeyForJwtMustBeAtLeast32Chars!!");
        other.init();
        String foreignToken = other.createAccessToken(member());

        assertThatThrownBy(() -> jwtUtil.getClaims(foreignToken))
                .isInstanceOf(JwtException.class);
    }
}
