package com.doumi.donation.board.service;

import com.doumi.donation.board.model.dto.Board;
import com.doumi.donation.board.model.dto.BoardListResponse;
import java.util.Map;

public interface BoardService {
    BoardListResponse getBoardList(String category, int page, int size);
    Board getBoardDetail(int id, Long userId);
    int registBoard(Board board, Long userId);
    int updateBoard(int id, Board board, Long userId);
    int deleteBoard(int id, Long userId);
    Map<String, Object> toggleBoardLike(int boardId, Long userId);
}
