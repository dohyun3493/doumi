package com.doumi.donation.notification.batch;

import com.doumi.donation.notification.model.dao.NotificationDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 찜한 캠페인의 마감 임박(D-3) 알림을 매일 생성 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationDao notificationDao;

    @Scheduled(cron = "0 0 9 * * *")    // 매일 09:00
    public void notifyDeadlineApproaching() {
        try {
            int created = notificationDao.insertDeadlineNotifications();
            log.info("마감 임박(D-3) 알림 생성: {}건", created);
        } catch (Exception e) {
            log.error("마감 임박 알림 배치 실패", e);
        }
    }
}
