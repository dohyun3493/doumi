package com.doumi.donation.board.model.dao;

import com.doumi.donation.board.model.dto.Comment;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentDao {
    List<Comment> getCommentsByBoardId(@Param("boardId") Long boardId);
    Comment selectCommentById(@Param("commentId") Long commentId);
    int insertComment(Comment comment);
    int updateComment(Comment comment);
    int softDeleteComment(@Param("commentId") Long commentId, @Param("deletedBy") String deletedBy);
}
