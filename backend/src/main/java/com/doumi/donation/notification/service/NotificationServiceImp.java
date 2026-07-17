package com.doumi.donation.notification.service;

import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.notification.model.dao.NotificationDao;
import com.doumi.donation.notification.model.dto.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImp implements NotificationService {

    private final NotificationDao notificationDao;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(long memberId) {
        return notificationDao.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public void markRead(long memberId, long notificationId) {
        int updated = notificationDao.markRead(notificationId, memberId);
        if (updated == 0) {
            throw new ResourceNotFoundException("존재하지 않는 알림입니다.");
        }
    }

    @Override
    @Transactional
    public void markAllRead(long memberId) {
        notificationDao.markAllRead(memberId);
    }

    @Override
    @Transactional
    public void deleteNotification(long memberId, long notificationId) {
        int deleted = notificationDao.deleteNotification(notificationId, memberId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("존재하지 않는 알림입니다.");
        }
    }

    @Override
    @Transactional
    public void sendToMember(long memberId, String content, String linkUrl) {
        notificationDao.insertNotification(memberId, "ADMIN_MESSAGE", content, linkUrl);
    }
}
