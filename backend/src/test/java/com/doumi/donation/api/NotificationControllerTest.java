package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.notification.controller.NotificationController;
import com.doumi.donation.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.doumi.donation.api.ApiTestSupport.로그인;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("알림 /api/notifications")
class NotificationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    NotificationService notificationService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/notifications → 200 내 알림 목록 (로그인)")
    void 내알림목록() throws Exception {
        when(notificationService.getMyNotifications(1L)).thenReturn(List.of());

        mvc.perform(get("/api/notifications").with(로그인()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read → 200 알림 읽음 처리 (로그인)")
    void 읽음처리() throws Exception {
        mvc.perform(patch("/api/notifications/3/read").with(로그인()))
                .andExpect(status().isOk());
        verify(notificationService).markRead(1L, 3L);
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all → 200 모든 알림 읽음 처리 (로그인)")
    void 전체읽음처리() throws Exception {
        mvc.perform(patch("/api/notifications/read-all").with(로그인()))
                .andExpect(status().isOk());
        verify(notificationService).markAllRead(1L);
    }
}
