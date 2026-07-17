package com.doumi.donation.notification.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 관리자 → 회원 직접 알림 전송 요청 */
@Data
public class NotifyRequest {

    @NotBlank(message = "알림 내용을 입력해 주세요.")
    private String content;

    /** 클릭 시 이동할 경로 (선택) */
    private String linkUrl;
}
