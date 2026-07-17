package com.doumi.donation.campaigns.batch;

import com.doumi.donation.campaigns.service.CampaignsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 날짜에 따라 캠페인 status를 자동 전환한다.
 * <ul>
 *   <li>시작일(start_date) 도래 → '모집예정' → '모집중'</li>
 *   <li>마감일(end_date) 경과 → '모집중' → '모집완료'</li>
 * </ul>
 *
 * <p>status를 DB에 실제로 갱신하므로 목록 화면 표시뿐 아니라
 * 기부 차단(모집중만 허용)·챗봇 추천 제외 등 다른 로직과도 상태가 일관되게 유지된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignStatusScheduler {

    private final CampaignsService campaignsService;

    /** 매일 00:10에 캠페인 상태 정리 (운영용 정기 실행) */
    @Scheduled(cron = "0 10 0 * * *")
    public void syncStatusesDaily() {
        syncStatuses();
    }

    /** 서버 기동 직후에도 1회 실행 → 재시작하면 즉시 반영(누락분 복구·데모 편의) */
    @EventListener(ApplicationReadyEvent.class)
    public void syncStatusesOnStartup() {
        syncStatuses();
    }

    private void syncStatuses() {
        try {
            // 순서 중요: 먼저 열고(open) 나중에 닫는다(close).
            // 시작·종료가 모두 지난 캠페인이 모집예정 → 모집중 → 모집완료로 올바르게 정착됨.
            int opened = campaignsService.openScheduledCampaigns();
            if (opened > 0) {
                log.info("시작일 도래 캠페인 {}건을 '모집중'으로 자동 전환했습니다.", opened);
            }
            int closed = campaignsService.closeExpiredCampaigns();
            if (closed > 0) {
                log.info("마감일 경과 캠페인 {}건을 '모집완료'로 자동 전환했습니다.", closed);
            }
        } catch (Exception e) {
            log.error("캠페인 상태 자동 전환 실패", e);
        }
    }
}
