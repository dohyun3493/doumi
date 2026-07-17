package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.config.jwt.JWTUtil;
import com.doumi.donation.member.controller.AuthController;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ※ 로그인(POST /api/v1/auth/login)은 컨트롤러가 아닌 필터(JWTAuthenticationFilter)에서
 * 처리되므로 여기서 테스트하지 못한다 → 서버 띄우고 실제 호출로 수동 테스트.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("인증 /api/v1/auth")
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JWTUtil jwtUtil;
    @MockitoBean
    MemberService memberService;

    @Test
    @DisplayName("POST /api/v1/auth/refresh → 200 토큰 재발급")
    void 토큰재발급() throws Exception {
        Claims claims = Jwts.claims();
        claims.put("email", "me@test.com");
        when(jwtUtil.getClaims("valid-refresh-token")).thenReturn(claims);

        Member member = new Member();
        member.setEmail("me@test.com");
        member.setRefresh("valid-refresh-token"); // DB에 저장된 토큰과 일치해야 통과
        when(memberService.findByEmail("me@test.com")).thenReturn(member);
        when(jwtUtil.createAccessToken(any())).thenReturn("new-access");
        when(jwtUtil.createRefreshToken(any())).thenReturn("new-refresh");

        mvc.perform(post("/api/v1/auth/refresh").header("Refresh-Token", "valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }
}
