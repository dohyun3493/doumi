package com.doumi.donation.config.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class JWTVerificationFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTVerificationFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 헤더에서 Authorization 값을 꺼냅니다.
        String authHeader = request.getHeader("Authorization");

        // 2. 헤더에 토큰이 들어있는지 확인
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer "을 자른 순수 토큰 값

            try {
                // 3. JWTUtil을 이용해 토큰을 파싱하고 유효성 검사
                Claims claims = jwtUtil.getClaims(token);
                String role = claims.get("role", String.class); // 토큰 내 저장된 권한 획득
                // memberId는 숫자라 환경에 따라 Integer/Long으로 올 수 있어 안전하게 변환
                Long memberId = Long.valueOf(claims.get("memberId").toString());

                // 4. 시큐리티 세션에 주입할 권한 리스트 생성 및 삽입
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                authorities.add(authority);

                // 5. 최종 시큐리티 인증 토큰 객체 조립 (principal = memberId)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(memberId, null, authorities);

                // 6. 스프링 시큐리티가 "인증 완료"로 보관할 수 있게 세션 컨텍스트에 박아넣음
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // 예외가 발생하면 상위 SecurityExceptionHandlingFilter가 캐치하여 처리하도록 예외를 상위로 던집니다.
                throw new io.jsonwebtoken.JwtException(e.getMessage(), e);
            }
        }

        // 3. 토큰이 없거나 무사히 처리되었다면 다음 필터로 물흐르듯 보냄
        filterChain.doFilter(request, response);
    }
}
