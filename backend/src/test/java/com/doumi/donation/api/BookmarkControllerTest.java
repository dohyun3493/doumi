package com.doumi.donation.api;

import com.doumi.donation.bookmark.controller.BookmarkController;
import com.doumi.donation.bookmark.service.BookmarkService;
import com.doumi.donation.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookmarkController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("북마크(찜) /api/bookmarks")
class BookmarkControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BookmarkService bookmarkService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/bookmarks → 200 내가 찜한 캠페인 목록 (로그인)")
    void 내찜목록() throws Exception {
        when(bookmarkService.getMyBookmarks(1L)).thenReturn(List.of());

        mvc.perform(get("/api/bookmarks").with(로그인()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/bookmarks/{campaignId} → 201 캠페인 찜 (로그인)")
    void 찜등록() throws Exception {
        mvc.perform(post("/api/bookmarks/5").with(로그인()))
                .andExpect(status().isCreated());
        verify(bookmarkService).addBookmark(1L, 5L);
    }

    @Test
    @DisplayName("DELETE /api/bookmarks/{campaignId} → 200 찜 해제 (로그인)")
    void 찜해제() throws Exception {
        mvc.perform(delete("/api/bookmarks/5").with(로그인()))
                .andExpect(status().isOk());
        verify(bookmarkService).removeBookmark(1L, 5L);
    }
}
