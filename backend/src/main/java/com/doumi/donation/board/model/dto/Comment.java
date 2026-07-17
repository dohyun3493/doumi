package com.doumi.donation.board.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    private Long commentId;
    private Long boardId;
    private Long authorId;
    private String authorName; // MEMBER 테이블과 JOIN하여 획득
    private String content;
    private String createdAt;
    private String deletedBy; // null=정상, 'AUTHOR'=작성자 삭제, 'ADMIN'=관리자 삭제
}
