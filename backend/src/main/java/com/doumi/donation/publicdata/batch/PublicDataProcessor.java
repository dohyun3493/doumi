package com.doumi.donation.publicdata.batch;

import com.doumi.donation.publicdata.model.dto.DonationProgramDto;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * DB 저장 전 필드 가공(포맷팅) 및 정제를 처리하는 Processor
 */
@Component
public class PublicDataProcessor implements ItemProcessor<DonationProgramDto, DonationProgramDto> {

    @Override
    public DonationProgramDto process(DonationProgramDto item) throws Exception {
        if (item.getCntrProgrmRegistNo() == null || item.getCntrProgrmRegistNo().isBlank()) {
            return null; // 모집등록번호가 없으면 스킵 (필터링)
        }
        sanitize(item);
        return item;
    }

    /** DB 저장 전 필드 정제 (날짜 변환, URL 검증, 금액 숫자화) */
    private void sanitize(DonationProgramDto dto) {
        dto.setRcritBgnde(toDate(dto.getRcritBgnde()));
        dto.setRcritEndde(toDate(dto.getRcritEndde()));
        dto.setHmpgAdres(sanitizeUrl(dto.getHmpgAdres()));
        dto.setRcritGoalAm(parseAmount(dto.getRcritGoalAm()));
    }

    /** yyyyMMdd → yyyy-MM-dd. 8자리가 아니면 null */
    private String toDate(String raw) {
        if (raw == null || raw.trim().length() != 8) return null;
        String s = raw.trim();
        return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8);
    }

    /** 중복 프로토콜 제거, 빈 URL → null */
    private String sanitizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String url = raw.trim().replaceAll("^(https?://)+(https?://)", "$1");
        if (url.equals("http://") || url.equals("https://")) return null;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null;
        return url;
    }

    /** 숫자 이외 문자 제거. 비어 있으면 null */
    private String parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = raw.trim().replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }
}
