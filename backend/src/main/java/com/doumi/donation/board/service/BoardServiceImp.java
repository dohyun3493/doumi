package com.doumi.donation.board.service;

import com.doumi.donation.board.model.dao.BoardDao;
import com.doumi.donation.board.model.dto.Board;
import com.doumi.donation.board.model.dto.BoardListResponse;
import com.doumi.donation.exception.UnauthorizedException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BoardServiceImp implements BoardService {
    private final BoardDao boardDao;
    private final MemberDao memberDao;

    @Autowired
    public BoardServiceImp(BoardDao boardDao, MemberDao memberDao){
        this.boardDao = boardDao;
        this.memberDao = memberDao;
    }

    @PostConstruct
    public void init() {
        try {
            boardDao.createBoardLikeTableIfNotExists();
        } catch (Exception e) {
            System.err.println("BOARD_LIKE 테이블 자동 생성 중 오류 (이미 생성되었을 수 있음): " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BoardListResponse getBoardList(String category, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        int offset = (page - 1) * size;

        Map<String, Object> params = new HashMap<>();
        params.put("category", category);
        params.put("offset", offset);
        params.put("size", size);

        long totalElements = boardDao.countBoards(category);
        List<Board> posts = boardDao.selectBoardList(params);
        int totalPages = (int) Math.ceil((double) totalElements / size);


        return BoardListResponse.builder()
                .posts(posts)
                .currentPage(page)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .build();
    }

    @Override
    @Transactional
    public Board getBoardDetail(int id, Long userId) {
        boardDao.updateView(id);
        Board board = boardDao.getBoardDetail(id);
        if (board != null && userId != null) {
            int exists = boardDao.checkLikeExists(id, userId);
            board.setLiked(exists > 0);
        }
        return board;
    }

    @Override
    @Transactional
    public int registBoard(Board board, Long userId) {
        board.setAuthorId(userId);
        // 공지(또는 상단 고정)는 관리자만 작성 가능 — 프론트 우회(직접 API 호출) 차단
        if (isNoticeOrPinned(board) && !isAdmin(userId)) {
            throw new UnauthorizedException("공지 작성은 관리자만 가능합니다.");
        }
        return boardDao.registBoard(board);
    }

    @Override
    @Transactional
    public int updateBoard(int id, Board board, Long userId) {
        Board origin = boardDao.getBoardDetail(id);
        if(origin == null) return 0;
        if(!(origin.getAuthorId().equals(userId))) return 0;
        // 일반 사용자가 수정으로 '공지'로 바꾸는 우회 차단
        if ("공지".equals(board.getCategory()) && !isAdmin(userId)) {
            throw new UnauthorizedException("공지로 변경은 관리자만 가능합니다.");
        }

        board.setBoardId((long) id);
        return boardDao.updateBoard(board);
    }

    @Override
    @Transactional
    public int deleteBoard(int id, Long userId) {
        Board origin = boardDao.getBoardDetail(id);

        if(origin == null) return 0;
        // 작성자 본인 또는 관리자만 삭제 가능 (관리자는 탈퇴 회원이 남긴 글도 정리 가능)
        if(!isOwnerOrAdmin(origin.getAuthorId(), userId)) return 0;

        return boardDao.deleteBoard(id);
    }

    // 작성자 본인이거나 ADMIN이면 true
    private boolean isOwnerOrAdmin(Long authorId, Long userId) {
        if (authorId != null && authorId.equals(userId)) return true;
        return isAdmin(userId);
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        Member member = memberDao.findByMemberId(userId);
        return member != null && "ADMIN".equals(member.getMemberType());
    }

    // 공지 카테고리이거나 상단 고정이면 관리자 전용 글
    private boolean isNoticeOrPinned(Board board) {
        return "공지".equals(board.getCategory()) || board.isPinned();
    }

    @Override
    @Transactional
    public Map<String, Object> toggleBoardLike(int boardId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        int exists = boardDao.checkLikeExists(boardId, userId);
        boolean liked;
        if (exists > 0) {
            boardDao.deleteLike(boardId, userId);
            boardDao.updateLikesCount(boardId, -1);
            liked = false;
        } else {
            boardDao.insertLike(boardId, userId);
            boardDao.updateLikesCount(boardId, 1);
            liked = true;
        }

        Board board = boardDao.getBoardDetail(boardId);
        int likesCount = board != null ? board.getLikes() : 0;

        result.put("liked", liked);
        result.put("likesCount", likesCount);
        return result;
    }
}
