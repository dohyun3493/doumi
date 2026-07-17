package com.doumi.donation.config.jwt;

import com.doumi.donation.member.model.dto.Member;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt.secret:defaultSecretKeyForDoumiDonationProject2026jwt}")
    private String secretKey;

    private Key key;

    // Access Token의 수명 설정 (30분)
    private final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 30;
    
    // Refresh Token의 수명 설정 (7일)
    private final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    @PostConstruct
    public void init() {
        byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    // 1. Access Token 생성 메소드
    public String createAccessToken(Member member) {
        long currentMillis = System.currentTimeMillis();
        long expireMillis = currentMillis + ACCESS_TOKEN_EXPIRATION;
        Date expireDate = new Date(expireMillis);

        JwtBuilder builder = Jwts.builder();
        builder.setSubject(member.getEmail());
        builder.claim("memberId", member.getMemberId());
        builder.claim("email", member.getEmail());
        builder.claim("role", "ROLE_" + member.getMemberType());
        builder.setIssuedAt(new Date(currentMillis));
        builder.setExpiration(expireDate);
        builder.signWith(key, SignatureAlgorithm.HS256);

        String token = builder.compact();
        return token;
    }

    // 2. Refresh Token 생성 메소드
    public String createRefreshToken(Member member) {
        long currentMillis = System.currentTimeMillis();
        long expireMillis = currentMillis + REFRESH_TOKEN_EXPIRATION;
        Date expireDate = new Date(expireMillis);

        JwtBuilder builder = Jwts.builder();
        builder.setSubject(member.getEmail());
        builder.claim("email", member.getEmail());
        builder.setIssuedAt(new Date(currentMillis));
        builder.setExpiration(expireDate);
        builder.signWith(key, SignatureAlgorithm.HS256);

        String token = builder.compact();
        return token;
    }

    // 3. 토큰 검증 및 해독 결과(Claims) 반환 메소드
    public Claims getClaims(String token) {
        JwtParserBuilder parserBuilder = Jwts.parserBuilder();
        parserBuilder.setSigningKey(key);
        JwtParser parser = parserBuilder.build();
        
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims;
    }
}
