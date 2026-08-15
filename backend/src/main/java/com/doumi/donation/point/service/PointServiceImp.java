package com.doumi.donation.point.service;

import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.notification.model.dao.NotificationDao;
import com.doumi.donation.point.model.dao.PointDao;
import com.doumi.donation.point.model.dto.ChargeRequest;
import com.doumi.donation.point.model.dto.PointHistory;
import com.doumi.donation.point.model.dto.UseRequest;
import com.doumi.donation.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class PointServiceImp implements PointService {

    private final MemberDao memberDao;
    private final PointDao pointDao;
    private final NotificationDao notificationDao;

    @Autowired
    public PointServiceImp(MemberDao memberDao, PointDao pointDao, NotificationDao notificationDao) {
        this.memberDao = memberDao;
        this.pointDao = pointDao;
        this.notificationDao = notificationDao;
    }

    @Override
    @Transactional
    public void charge(long memberId, ChargeRequest req) {
        Member member = memberDao.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }

        PointHistory history = new PointHistory();
        history.setMemberId(memberId);
        history.setType("CHARGE");
        history.setAmount(req.getAmount());

        pointDao.insertHistory(history);
        memberDao.updatePointBalanceById(memberId, req.getAmount());
    }

    // 기부가 발생하면 총 기부금 등 통계가 바뀌므로 통계 캐시를 즉시 무효화
    @Override
    @CacheEvict(cacheNames = CacheConfig.STATS_CACHE, allEntries = true)
    @Transactional
    public void use(long memberId, UseRequest req) {
        Member member = memberDao.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }

        // 행 잠금(FOR UPDATE) 조회 — 동시 기부 시에도 상태/금액 정합성 보장
        Campaign campaign = pointDao.findCampaignForDonation(req.getCampaignId());
        if (campaign == null) {
            throw new ResourceNotFoundException("존재하지 않는 캠페인입니다.");
        }
        if (!"모집중".equals(campaign.getStatus())) {
            throw new IllegalArgumentException("모집 중인 캠페인에만 기부할 수 있습니다.");
        }

        // 잔액 확인과 차감을 한 쿼리로 (동시 요청에 의한 마이너스 잔액 방지)
        int updated = memberDao.decreasePointIfEnough(memberId, req.getAmount());
        if (updated == 0) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        PointHistory history = new PointHistory();
        history.setMemberId(memberId);
        history.setType("USE");
        history.setAmount(req.getAmount());

        pointDao.insertDonation(memberId, req.getCampaignId(), req.getAmount());
        pointDao.insertHistory(history);
        pointDao.updateCampaignAmount(req.getCampaignId(), req.getAmount());

        // 이번 기부로 목표 금액을 막 넘긴 순간이면, 찜한 회원들에게 달성 알림
        boolean goalJustAchieved = campaign.getGoalAmount() != null && campaign.getCurrentAmount() != null
                && campaign.getCurrentAmount() < campaign.getGoalAmount()
                && campaign.getCurrentAmount() + req.getAmount() >= campaign.getGoalAmount();
        if (goalJustAchieved) {
            try {
                notificationDao.insertGoalAchievedNotifications(req.getCampaignId());
            } catch (Exception e) {
                // 알림 실패가 기부 자체를 실패시키지 않도록 격리
                log.error("목표 달성 알림 생성 실패: campaignId={}", req.getCampaignId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void refund(long memberId, long amount) {
        // 잔액 확인과 회수를 한 쿼리로 — 충전했던 포인트를 이미 기부 등으로 써버렸다면
        // 잔액이 부족해 환불 불가 (마이너스 잔액 방지)
        int updated = memberDao.decreasePointIfEnough(memberId, amount);
        if (updated == 0) {
            throw new IllegalArgumentException("이미 사용한 포인트가 있어 충전 취소(환불)가 불가능합니다.");
        }

        PointHistory history = new PointHistory();
        history.setMemberId(memberId);
        history.setType("REFUND");
        history.setAmount(amount);
        pointDao.insertHistory(history);
    }

    @Override
    @Transactional
    public void refundToMember(long memberId, long amount) {
        // 기부가 무효화되어 기부자에게 포인트를 돌려준다 (잔액 증가 + REFUND 이력)
        memberDao.updatePointBalanceById(memberId, amount);

        PointHistory history = new PointHistory();
        history.setMemberId(memberId);
        history.setType("REFUND");
        history.setAmount(amount);
        pointDao.insertHistory(history);
    }

    @Override
    @Transactional
    public void revertRefund(long memberId, long amount) {
        // 토스 결제취소가 실패해 refund()로 회수했던 포인트를 되돌린다.
        // 외부 호출이 트랜잭션 밖에 있어 자동 롤백이 안 되므로 명시적으로 보상한다.
        // (회수를 무효화하는 것이므로 이력은 충전과 동일한 CHARGE로 남긴다)
        memberDao.updatePointBalanceById(memberId, amount);

        PointHistory history = new PointHistory();
        history.setMemberId(memberId);
        history.setType("CHARGE");
        history.setAmount(amount);
        pointDao.insertHistory(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointHistory> getHistory(long memberId) {
        return pointDao.findHistoryByMemberId(memberId);
    }
}
