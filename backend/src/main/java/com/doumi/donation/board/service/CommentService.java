package com.doumi.donation.board.service;

import java.util.List;

import com.doumi.donation.board.model.dto.Comment;
import com.doumi.donation.board.model.dto.CommentRequest;

public interface CommentService {
    List<Comment> getCommentsByBoardId(Long boardId);
    int registComment(Long boardId, Long userId, CommentRequest request);
    int updateComment(Long commentId, Long userId, CommentRequest request);
    int deleteComment(Long commentId, Long userId);
}
