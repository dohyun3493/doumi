package com.doumi.donation.api;

import com.doumi.donation.board.controller.CommentController;
import com.doumi.donation.board.service.CommentService;
import com.doumi.donation.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("댓글 /api/board/**/comments")
class CommentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CommentService commentService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/board/{boardId}/comments → 200 댓글 목록")
    void 목록조회() throws Exception {
        when(commentService.getCommentsByBoardId(1L)).thenReturn(List.of());

        mvc.perform(get("/api/board/1/comments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/board/{boardId}/comments → 201 댓글 등록 (로그인)")
    void 등록() throws Exception {
        mvc.perform(post("/api/board/1/comments").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"좋은 캠페인이네요!"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/board/comments/{commentId} → 200 댓글 수정 (작성자)")
    void 수정() throws Exception {
        mvc.perform(put("/api/board/comments/1").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정된 댓글"}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/board/comments/{commentId} → 200 댓글 삭제 (작성자)")
    void 삭제() throws Exception {
        mvc.perform(delete("/api/board/comments/1").with(로그인()))
                .andExpect(status().isOk());
    }
}
