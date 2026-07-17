package com.doumi.donation.member.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemberJoinRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "INDIVIDUAL|ORGANIZATION", message = "memberType은 INDIVIDUAL 또는 ORGANIZATION이어야 합니다.")
    private String memberType;

    /** 단체 고유번호 (ORGANIZATION 가입 시 필수, 형식: 123-45-67890) */
    private String orgRegNo;
}
