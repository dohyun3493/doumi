package com.doumi.donation.board.model.dao;

import com.doumi.donation.board.model.dto.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BoardDao {
    long countBoards(@Param("category") String category);
    List<Board> selectBoardList(Map<String, Object> params);
    Board getBoardDetail(@Param("id") int id);
    void updateView(@Param("id") int id);
    int registBoard(Board board);
    int updateBoard(Board board);
    int deleteBoard(@Param("id") int id);
    
    // 좋아요 관련 메소드 추가
    int checkLikeExists(@Param("boardId") int boardId, @Param("memberId") Long memberId);
    int insertLike(@Param("boardId") int boardId, @Param("memberId") Long memberId);
    int deleteLike(@Param("boardId") int boardId, @Param("memberId") Long memberId);
    int updateLikesCount(@Param("boardId") int boardId, @Param("amount") int amount);
    void createBoardLikeTableIfNotExists();
}
