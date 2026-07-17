package com.doumi.donation.member.model.dto;

import lombok.Data;

@Data
public class MemberLoginRequest {
    private String email;
    private String password;
}
