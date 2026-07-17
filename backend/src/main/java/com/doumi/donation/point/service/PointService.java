package com.doumi.donation.point.service;

import com.doumi.donation.point.model.dto.ChargeRequest;
import com.doumi.donation.point.model.dto.PointHistory;
import com.doumi.donation.point.model.dto.UseRequest;

import java.util.List;

public interface PointService {
    void charge(long memberId, ChargeRequest req);
    void use(long memberId, UseRequest req);
    /** 충전 취소(환불) 시 충전했던 포인트를 회수. 이미 사용해 잔액이 부족하면 예외 */
    void refund(long memberId, long amount);
    /** 단체 탈퇴 등으로 기부가 무효화될 때 기부자에게 포인트를 되돌려준다 (잔액 +금액 + REFUND 이력) */
    void refundToMember(long memberId, long amount);
    List<PointHistory> getHistory(long memberId);
}
