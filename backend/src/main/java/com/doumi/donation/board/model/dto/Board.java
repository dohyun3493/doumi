package com.doumi.donation.board.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {
    private Long boardId;
    private Long campaignId;

    private Long authorId;
    
    private String authorName; // MEMBER 테이블과 JOIN하여 획득

    @NotBlank(message = "카테고리는 필수 입력 항목입니다.")
    private String category;

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 200, message = "제목은 최대 200자까지 작성할 수 있습니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력 항목입니다.")
    private String content;
    
    private int views;
    private int likes;
    
    @JsonProperty("isPinned")
    private boolean isPinned;
    
    private String createdAt;
    private int commentsCount; // 목록 조회 시 댓글 개수 합산
    
    @JsonProperty("isLiked")
    private boolean isLiked; // 로그인한 사용자의 좋아요 여부
}
