package com.doumi.donation.board.service;

import com.doumi.donation.board.model.dao.BoardDao;
import com.doumi.donation.board.model.dao.CommentDao;
import com.doumi.donation.board.model.dto.Board;
import com.doumi.donation.board.model.dto.Comment;
import com.doumi.donation.board.model.dto.CommentRequest;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImp implements CommentService {
    private final CommentDao commentDao;
    private final BoardDao boardDao;
    private final MemberDao memberDao;

    @Autowired
    public CommentServiceImp(CommentDao commentDao, BoardDao boardDao, MemberDao memberDao) {
        this.commentDao = commentDao;
        this.boardDao = boardDao;
        this.memberDao = memberDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByBoardId(Long boardId) {
        // 게시글 존재 여부 검증
        Board board = boardDao.getBoardDetail(boardId.intValue());
        if (board == null) {
            throw new ResourceNotFoundException("해당 게시글을 찾을 수 없습니다. (ID: " + boardId + ")");
        }
        return commentDao.getCommentsByBoardId(boardId);
    }

    @Override
    @Transactional
    public int registComment(Long boardId, Long userId, CommentRequest request) {
        // 게시글 존재 여부 검증
        Board board = boardDao.getBoardDetail(boardId.intValue());
        if (board == null) {
            throw new ResourceNotFoundException("해당 게시글을 찾을 수 없습니다. (ID: " + boardId + ")");
        }

        Comment comment = Comment.builder()
                .boardId(boardId)
                .authorId(userId)
                .content(request.getContent())
                .build();
        return commentDao.insertComment(comment);
    }

    @Override
    @Transactional
    public int updateComment(Long commentId, Long userId, CommentRequest request) {
        Comment origin = commentDao.selectCommentById(commentId);
        if (origin == null) {
            throw new ResourceNotFoundException("해당 댓글을 찾을 수 없습니다. (ID: " + commentId + ")");
        }
        if (origin.getDeletedBy() != null) {
            throw new IllegalArgumentException("삭제된 댓글은 수정할 수 없습니다.");
        }
        if (!origin.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("댓글을 수정할 권한이 없습니다.");
        }

        Comment comment = Comment.builder()
                .commentId(commentId)
                .content(request.getContent())
                .build();
        return commentDao.updateComment(comment);
    }

    @Override
    @Transactional
    public int deleteComment(Long commentId, Long userId) {
        Comment origin = commentDao.selectCommentById(commentId);
        if (origin == null) {
            throw new ResourceNotFoundException("해당 댓글을 찾을 수 없습니다. (ID: " + commentId + ")");
        }
        if (origin.getDeletedBy() != null) {
            throw new IllegalArgumentException("이미 삭제된 댓글입니다.");
        }

        // 작성자 본인 또는 관리자만 삭제 가능. 삭제 주체를 기록해 표시에 활용
        boolean isAuthor = origin.getAuthorId().equals(userId);
        boolean isAdmin = !isAuthor && isAdmin(userId);
        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedException("댓글을 삭제할 권한이 없습니다.");
        }

        // soft delete: 행을 지우지 않고 삭제 주체만 기록 → "OO에 의해 삭제된 댓글입니다" 안내용
        return commentDao.softDeleteComment(commentId, isAuthor ? "AUTHOR" : "ADMIN");
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        Member member = memberDao.findByMemberId(userId);
        return member != null && "ADMIN".equals(member.getMemberType());
    }
}
