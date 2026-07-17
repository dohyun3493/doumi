package com.doumi.donation.board.controller;

import com.doumi.donation.board.model.dto.Comment;
import com.doumi.donation.board.model.dto.CommentRequest;
import com.doumi.donation.board.service.CommentService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/board")
public class CommentController {
    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // 댓글 조회
    @GetMapping("/{boardId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long boardId) {
        List<Comment> result = commentService.getCommentsByBoardId(boardId);
        return ResponseEntity.ok(result);
    }

    // 댓글 등록
    @PostMapping("/{boardId}/comments")
    public ResponseEntity<?> registComment(@PathVariable Long boardId,
                                           @AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody CommentRequest commentRequest) {
        commentService.registComment(boardId, userId, commentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 댓글 수정
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody CommentRequest commentRequest) {
        commentService.updateComment(commentId, userId, commentRequest);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
