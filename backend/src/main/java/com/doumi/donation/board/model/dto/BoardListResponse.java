package com.doumi.donation.board.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardListResponse {
    private List<Board> posts;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}
