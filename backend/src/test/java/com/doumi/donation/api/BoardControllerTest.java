package com.doumi.donation.api;

import com.doumi.donation.board.controller.BoardController;
import com.doumi.donation.board.model.dto.Board;
import com.doumi.donation.board.model.dto.BoardListResponse;
import com.doumi.donation.board.service.BoardService;
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
import java.util.Map;

import static com.doumi.donation.api.ApiTestSupport.로그인;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BoardController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("게시판 /api/board")
class BoardControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BoardService boardService;

    @AfterEach
    void 인증초기화() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/board → 200 목록 조회(페이징)")
    void 목록조회() throws Exception {
        when(boardService.getBoardList(any(), anyInt(), anyInt()))
                .thenReturn(BoardListResponse.builder()
                        .posts(List.of()).currentPage(1).totalPages(1).totalElements(0).build());

        mvc.perform(get("/api/board").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1));
    }

    @Test
    @DisplayName("GET /api/board/{id} → 200 상세 조회")
    void 상세조회() throws Exception {
        when(boardService.getBoardDetail(eq(1), any()))
                .thenReturn(Board.builder().boardId(1L).title("후기").content("내용").category("후기").build());

        mvc.perform(get("/api/board/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardId").value(1));
    }

    @Test
    @DisplayName("POST /api/board → 201 게시글 등록 (로그인)")
    void 등록() throws Exception {
        when(boardService.registBoard(any(), eq(1L))).thenReturn(1);

        mvc.perform(post("/api/board").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"후기","title":"제목","content":"내용"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/board/{id} → 200 게시글 수정 (작성자)")
    void 수정() throws Exception {
        when(boardService.updateBoard(eq(1), any(), eq(1L))).thenReturn(1);

        mvc.perform(put("/api/board/1").with(로그인())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"후기","title":"수정","content":"수정 내용"}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/board/{id} → 200 게시글 삭제 (작성자)")
    void 삭제() throws Exception {
        when(boardService.deleteBoard(eq(1), eq(1L))).thenReturn(1);

        mvc.perform(delete("/api/board/1").with(로그인()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/board/{id}/like → 200 좋아요 토글 (로그인)")
    void 좋아요() throws Exception {
        when(boardService.toggleBoardLike(1, 1L))
                .thenReturn(Map.of("liked", true, "likesCount", 3));

        mvc.perform(post("/api/board/1/like").with(로그인()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));
    }
}
