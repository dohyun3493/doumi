package com.doumi.donation.publicdata.batch;

import com.doumi.donation.publicdata.model.dto.DonationProgramDto;
import com.doumi.donation.publicdata.service.PublicDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 대량의 데이터를 비즈니스 서비스 레이어를 통해 DB에 upsert하는 Writer
 */
@Slf4j
@Component
public class PublicDataWriter implements ItemWriter<DonationProgramDto> {

    private final PublicDataService publicDataService;

    public PublicDataWriter(PublicDataService publicDataService) {
        this.publicDataService = publicDataService;
    }

    @Override
    public void write(Chunk<? extends DonationProgramDto> chunk) throws Exception {
        List<DonationProgramDto> items = new ArrayList<>(chunk.getItems());
        log.info("서비스 벌크 라이터 실행 - 건수: {}건", items.size());
        publicDataService.upsertCampaigns(items);
        log.info("서비스 벌크 적재 완료");
    }
}

