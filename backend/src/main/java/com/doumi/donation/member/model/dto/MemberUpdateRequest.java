package com.doumi.donation.member.model.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class MemberUpdateRequest {

    @Email
    private String email;

    private String password;

    private String name;

    // 프로필 사진 경로 (/uploads/...). 빈 문자열이면 사진 제거.
    private String profileImageUrl;
}
