package com.doumi.donation.notification.model.dao;

import com.doumi.donation.notification.model.dto.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationDao {
    List<Notification> findByMemberId(@Param("memberId") long memberId);
    int markRead(@Param("notificationId") long notificationId, @Param("memberId") long memberId);
    void markAllRead(@Param("memberId") long memberId);
    int deleteNotification(@Param("notificationId") long notificationId, @Param("memberId") long memberId);

    /** 마감 3일 전 캠페인을 찜한 회원들에게 알림 생성 (매일 배치). 생성 건수 반환 */
    int insertDeadlineNotifications();

    /** 목표 금액을 달성한 캠페인을 찜한 회원들에게 알림 생성 (기부 시점 실시간) */
    void insertGoalAchievedNotifications(@Param("campaignId") long campaignId);

    /** 단건 알림 생성 (단체 승인 결과·관리자 직접 알림 등 범용) */
    void insertNotification(@Param("memberId") long memberId,
                            @Param("type") String type,
                            @Param("content") String content,
                            @Param("linkUrl") String linkUrl);

    /** 사용 보고가 등록된 캠페인의 기부자 전원에게 알림 생성 */
    void insertReportNotifications(@Param("campaignId") long campaignId);
}
