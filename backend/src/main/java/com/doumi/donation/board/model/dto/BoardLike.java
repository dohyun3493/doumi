package com.doumi.donation.board.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardLike {
    private Long likeId;
    private Long boardId;
    private Long memberId;
    private String createdAt;
}
