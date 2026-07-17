package com.doumi.donation.notification.model.dto;

import lombok.Data;

@Data
public class Notification {
    private long notificationId;
    private long memberId;
    private String type;        // DEADLINE | GOAL_ACHIEVED | ADMIN_MESSAGE 등
    private String content;
    private String linkUrl;     // 클릭 시 이동할 프론트 경로 (예: /campaigns/7)
    private boolean read;       // is_read 컬럼 (SQL에서 alias로 매핑)
    private String createdAt;
}
