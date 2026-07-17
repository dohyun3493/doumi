package com.doumi.donation.member.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 관리자 회원 목록 페이징 응답 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPageResponse {
    private List<Member> members;
    private int currentPage;
    private int totalPages;
    private long totalElements;
}
