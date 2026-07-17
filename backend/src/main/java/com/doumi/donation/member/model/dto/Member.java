package com.doumi.donation.member.model.dto;

import lombok.Data;

@Data
public class Member {
    private long memberId;
    private String email;
    private String password;
    private String name;
    private String memberType; // INDIVIDUAL, ORGANIZATION
    private long pointBalance;
    private String createdAt;
    private String refresh;
    private String profileImageUrl;   // 프로필 사진 경로 (/uploads/...)

    // 단체 회원 전용 (개인 회원은 null)
    private String orgRegNo;        // 단체 고유번호
    private String orgStatus;       // PENDING | APPROVED | REJECTED
    private String rejectReason;    // 승인 거절 사유

    private String deletedAt;       // 탈퇴 일시 (NULL이면 활성 회원)
}
