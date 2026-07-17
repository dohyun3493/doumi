package com.doumi.donation.notification.service;

import com.doumi.donation.notification.model.dto.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getMyNotifications(long memberId);
    void markRead(long memberId, long notificationId);
    void markAllRead(long memberId);
    void deleteNotification(long memberId, long notificationId);

    /** 관리자가 특정 회원에게 직접 알림 전송 */
    void sendToMember(long memberId, String content, String linkUrl);
}
