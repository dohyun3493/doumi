package com.doumi.donation.board.controller;

import com.doumi.donation.board.model.dto.Board;
import com.doumi.donation.board.model.dto.BoardListResponse;
import com.doumi.donation.board.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @Autowired
    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<?> getBoardList(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        BoardListResponse response = boardService.getBoardList(category, page, size);

        return ResponseEntity.ok(response);
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getBoardDetail(@PathVariable int id, @AuthenticationPrincipal Long userId) {
        Board board = boardService.getBoardDetail(id, userId);

        if (board != null) {
            return ResponseEntity.status(HttpStatus.OK).body(board);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 게시글 등록
    @PostMapping
    public ResponseEntity<?> registBoard(@AuthenticationPrincipal Long userId, @Valid @RequestBody Board board) {
        int result = boardService.registBoard(board, userId);

        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("게시글 등록에 실패했습니다.");
        }
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable int id,
                                  @Valid @RequestBody Board board,
                                  @AuthenticationPrincipal Long userId){
        int result = boardService.updateBoard(id, board, userId);

        if (result > 0) {
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable int id,
                                         @AuthenticationPrincipal Long userId){
        int result = boardService.deleteBoard(id, userId);

        if(result > 0){
            return ResponseEntity.status(HttpStatus.OK).build();
        } else{
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // 게시글 좋아요 토글
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable int id, @AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 서비스입니다.");
        }
        Map<String, Object> result = boardService.toggleBoardLike(id, userId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
