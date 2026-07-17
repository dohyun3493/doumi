package com.doumi.donation.publicdata.batch;

import com.doumi.donation.publicdata.model.dto.DonationProgramDto;
import com.doumi.donation.publicdata.service.PublicDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.List;

/**
 * API 페이징 조회를 위한 상태 유지(Stateful) Custom Reader
 */
@Slf4j
public class PublicDataApiItemReader implements ItemReader<DonationProgramDto> {

    private final PublicDataService publicDataService;
    private int currentPage = 1;
    private final int pageSize = 10;
    private final int maxItemCount = 20;  // 최대 수집 건수 제한
    private int readCount = 0;
    private List<DonationProgramDto> currentChunk;
    private int nextIndex = 0;
    private int totalCount = -1;
    private boolean isFinished = false;

    public PublicDataApiItemReader(PublicDataService publicDataService) {
        this.publicDataService = publicDataService;
    }

    public DonationProgramDto read() throws Exception {
        if (isFinished) {
            return null;
        }

        // 최대 수집 건수 도달 시 종료
        if (readCount >= maxItemCount) {
            isFinished = true;
            log.info("최대 수집 건수({})에 도달하여 조회를 종료합니다. (읽은 건수: {}건)", maxItemCount, readCount);
            return null;
        }

        if (currentChunk == null || nextIndex >= currentChunk.size()) {
            if (totalCount != -1 && (currentPage - 1) * pageSize >= totalCount) {
                isFinished = true;
                log.info("공공데이터 API 조회 완료 (총 {}건)", readCount);
                return null;
            }

            log.info("공공데이터 API 요청 - Page: {}, Size: {}", currentPage, pageSize);
            DonationProgramDto.ApiResponse response = publicDataService.fetchDonationPrograms(currentPage, pageSize);
            
            if (response == null || response.getBody() == null) {
                log.warn("API 응답이 비어있습니다. 종료합니다.");
                isFinished = true;
                return null;
            }

            if (totalCount == -1) {
                totalCount = response.getBody().getTotalCount();
                log.info("공공데이터 API 전체 대상 건수: {}건 (최대 {}건만 수집)", totalCount, maxItemCount);
            }

            if (response.getBody().getItems() == null || response.getBody().getItems().getItem() == null) {
                log.info("가져온 데이터 아이템이 없습니다. 종료합니다.");
                isFinished = true;
                return null;
            }

            currentChunk = response.getBody().getItems().getItem();
            nextIndex = 0;
            currentPage++;

            if (currentChunk.isEmpty()) {
                isFinished = true;
                return null;
            }
        }

        readCount++;
        return currentChunk.get(nextIndex++);
    }
}
