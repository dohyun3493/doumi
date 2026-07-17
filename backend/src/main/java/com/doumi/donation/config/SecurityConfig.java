package com.doumi.donation.config;

import com.doumi.donation.config.jwt.JWTAuthenticationFilter;
import com.doumi.donation.config.jwt.JWTVerificationFilter;
import com.doumi.donation.config.jwt.SecurityExceptionHandlingFilter;
import com.doumi.donation.config.jwt.JWTUtil;
import com.doumi.donation.member.service.MemberService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JWTUtil jwtUtil;
    private final MemberService memberService;
    private final AuthenticationConfiguration authenticationConfiguration;

    public SecurityConfig(JWTUtil jwtUtil, MemberService memberService, AuthenticationConfiguration authenticationConfiguration) {
        this.jwtUtil = jwtUtil;
        this.memberService = memberService;
        this.authenticationConfiguration = authenticationConfiguration;
    }

    // 1. AuthenticationManager 빈(Bean) 등록 설정
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        AuthenticationManager manager = configuration.getAuthenticationManager();
        return manager;
    }

    // 3. 보안 필터 체인 규칙 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 인증 매니저 객체 가져오기
        AuthenticationManager authManager = authenticationManager(authenticationConfiguration);

        // 시큐리티 기본 기능 비활성화 (API 서버이므로)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource())); // CORS 설정
        http.csrf(csrf -> csrf.disable());                                     // CSRF 방지 비활성화
        http.httpBasic(basic -> basic.disable());                             // 브라우저 팝업 로그인 비활성화
        http.formLogin(form -> form.disable());                               // 기본 폼 로그인 화면 비활성화

        // 세션을 완전히 사용하지 않는 STATELESS 모드 지정 (JWT 전용)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // URL별 접근 권한 규칙 설정
        http.authorizeHttpRequests(auth -> auth
                // CORS preflight 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ===== 인증 불필요 (공개) =====
                // 로그인 / 토큰 재발급 / 공공데이터 / 에러
                .requestMatchers("/api/v1/auth/**", "/api/public-data/**", "/error").permitAll()
                // 업로드된 이미지 정적 조회는 공개
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                // 회원가입 (POST /api/members)
                .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                // 조회성 GET 요청은 비로그인도 허용 (캠페인/통계/랭킹/게시글 보기)
                .requestMatchers(HttpMethod.GET,
                        "/api/campaigns/**", "/api/stats/**", "/api/rankings/**", "/api/board/**").permitAll()
                // AI 챗봇 대화는 비로그인도 허용 (캠페인 추천)
                .requestMatchers(HttpMethod.POST, "/api/chatbot/**").permitAll()

                // ===== 권한 필요 =====
                // 캠페인 등록: 단체(ORGANIZATION) 또는 관리자(ADMIN)만
                .requestMatchers(HttpMethod.POST, "/api/campaigns").hasAnyRole("ORGANIZATION", "ADMIN")
                // 캠페인 수정: 단체(ORGANIZATION) 또는 관리자(ADMIN)만
                .requestMatchers(HttpMethod.PATCH, "/api/campaigns/**").hasAnyRole("ORGANIZATION", "ADMIN")
                // 캠페인 삭제: 관리자(ADMIN)만
                .requestMatchers(HttpMethod.DELETE, "/api/campaigns/**").hasRole("ADMIN")

                // 시스템 관리자 전용 (공공데이터 배치 등 운영 작업)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ===== 그 외 모든 요청은 로그인(JWT) 필수 =====
                // 포인트 충전/사용, 결제, 글쓰기/수정/삭제, 회원정보 조회/수정/탈퇴, 캠페인 등록/수정/삭제 등
                .anyRequest().authenticated()
        );

        // 필터 조립 등록 (수업 자료 슬라이드 구조 일치)
        JWTVerificationFilter jwtVerifyFilter = new JWTVerificationFilter(jwtUtil);
        JWTAuthenticationFilter authFilter = new JWTAuthenticationFilter(authManager, jwtUtil, memberService);
        SecurityExceptionHandlingFilter exceptionFilter = new SecurityExceptionHandlingFilter();

        http.addFilterBefore(jwtVerifyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(authFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(exceptionFilter, JWTVerificationFilter.class);

        return http.build();
    }

    // CORS 우회 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
