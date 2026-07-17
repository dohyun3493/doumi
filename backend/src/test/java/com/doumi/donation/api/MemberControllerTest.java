package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.member.controller.MemberController;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.doumi.donation.api.ApiTestSupport.로그인;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("회원 /api/members")
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MemberService memberService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    /** memberId=1, email=me@test.com 인 회원 */
    private Member 나() {
        Member m = new Member();
        m.setMemberId(1L);
        m.setEmail("me@test.com");
        m.setName("테스터");
        m.setMemberType("INDIVIDUAL");
        return m;
    }

    @Test
    @DisplayName("POST /api/members → 201 회원가입")
    void 회원가입() throws Exception {
        mvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"me@test.com","password":"pw1234","name":"테스터","memberType":"INDIVIDUAL"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/members/{email} → 200 내 정보 조회")
    void 내정보조회() throws Exception {
        when(memberService.findByMemberId(1L)).thenReturn(나());
        when(memberService.findByEmail("me@test.com")).thenReturn(나());

        mvc.perform(get("/api/members/me@test.com").with(로그인()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.com"));
    }

    @Test
    @DisplayName("GET /api/members/{email}/donations → 200 내 기부 내역")
    void 내기부내역() throws Exception {
        when(memberService.findByMemberId(1L)).thenReturn(나());
        when(memberService.findDonationsByEmail("me@test.com")).thenReturn(List.of());

        mvc.perform(get("/api/members/me@test.com/donations").with(로그인()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/members/{email} → 200 내 정보 수정")
    void 내정보수정() throws Exception {
        when(memberService.findByMemberId(1L)).thenReturn(나());

        mvc.perform(patch("/api/members/me@test.com").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"새이름"}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/members/{email} → 200 회원 탈퇴")
    void 회원탈퇴() throws Exception {
        when(memberService.findByMemberId(1L)).thenReturn(나());

        mvc.perform(delete("/api/members/me@test.com").with(로그인()))
                .andExpect(status().isOk());
    }
}
