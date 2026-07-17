package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.member.controller.OrganizationAdminController;
import com.doumi.donation.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /api/admin/** 은 시큐리티에서 ADMIN 권한을 강제하므로, 여기서는 매핑·동작만 검증 */
@WebMvcTest(controllers = OrganizationAdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("단체 승인 관리 /api/admin/organizations")
class OrganizationAdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MemberService memberService;

    @Test
    @DisplayName("GET /api/admin/organizations → 200 단체 목록 조회 (관리자, status 필터 가능)")
    void 단체목록() throws Exception {
        when(memberService.getOrganizations(any())).thenReturn(List.of());

        mvc.perform(get("/api/admin/organizations").param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/admin/organizations/{memberId}/approve → 200 단체 승인 (관리자)")
    void 단체승인() throws Exception {
        mvc.perform(patch("/api/admin/organizations/5/approve"))
                .andExpect(status().isOk());
        verify(memberService).approveOrganization(5L);
    }

    @Test
    @DisplayName("PATCH /api/admin/organizations/{memberId}/reject → 200 단체 거절 (관리자, 사유 선택)")
    void 단체거절() throws Exception {
        mvc.perform(patch("/api/admin/organizations/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"서류 미비"}"""))
                .andExpect(status().isOk());
        verify(memberService).rejectOrganization(5L, "서류 미비");
    }
}
