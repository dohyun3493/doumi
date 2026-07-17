package com.doumi.donation.point.service;

import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.notification.model.dao.NotificationDao;
import com.doumi.donation.point.model.dao.PointDao;
import com.doumi.donation.point.model.dto.PointHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 환불(refund)")
class PointServiceImpTest {

    @Mock
    private MemberDao memberDao;

    @Mock
    private PointDao pointDao;

    @Mock
    private NotificationDao notificationDao;

    @InjectMocks
    private PointServiceImp service;

    @Test
    @DisplayName("잔액이 충분하면 포인트를 회수하고 REFUND 내역을 남긴다")
    void refundSucceeds() {
        // 차감 1건 성공
        when(memberDao.decreasePointIfEnough(1L, 10000L)).thenReturn(1);

        service.refund(1L, 10000L);

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointDao).insertHistory(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("REFUND");
        assertThat(captor.getValue().getAmount()).isEqualTo(10000L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 사용해 잔액이 부족하면 예외이며 내역을 남기지 않는다")
    void refundFailsWhenBalanceInsufficient() {
        // 차감 0건 (point_balance >= amount 조건 불충족)
        when(memberDao.decreasePointIfEnough(1L, 10000L)).thenReturn(0);

        assertThatThrownBy(() -> service.refund(1L, 10000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환불");

        verify(pointDao, never()).insertHistory(any());
    }
}
