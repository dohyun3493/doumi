package com.doumi.donation.publicdata.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import java.util.List;

/**
 * 행정안전부_기부관련단체정보서비스 (getCntrProgramList) 응답 item 매핑 DTO
 * 실제 XML 태그명 기준으로 작성
 */
@Data
public class DonationProgramDto {

    @JacksonXmlProperty(localName = "cntrProgrmRegistNo")
    private String cntrProgrmRegistNo;   // 모집등록번호

    @JacksonXmlProperty(localName = "rcritrNm")
    private String rcritrNm;             // 모집단체명

    @JacksonXmlProperty(localName = "rcritSj")
    private String rcritSj;              // 모집제목

    @JacksonXmlProperty(localName = "rcritPurps")
    private String rcritPurps;           // 모집목적

    @JacksonXmlProperty(localName = "rcritBgnde")
    private String rcritBgnde;           // 모집시작일

    @JacksonXmlProperty(localName = "rcritEndde")
    private String rcritEndde;           // 모집완료일

    @JacksonXmlProperty(localName = "rcritGoalAm")
    private String rcritGoalAm;          // 모집목표액

    @JacksonXmlProperty(localName = "rcritArea")
    private String rcritArea;            // 모집지역

    @JacksonXmlProperty(localName = "cntrClUpNm")
    private String cntrClUpNm;           // 기부 분야

    @JacksonXmlProperty(localName = "step")
    private String step;                 // 진행상태

    @JacksonXmlProperty(localName = "hmpgAdres")
    private String hmpgAdres;            // 홈페이지주소

    @JacksonXmlProperty(localName = "zip")
    private String zip;                  // 우편번호

    @JacksonXmlProperty(localName = "adres")
    private String adres;                // 주소

    @JacksonXmlProperty(localName = "dmstcTelno")
    private String dmstcTelno;           // 전화번호

    @JacksonXmlProperty(localName = "reprsntSj")
    private String reprsntSj;            // 대표 제목

    @JacksonXmlProperty(localName = "rcritrId")
    private String rcritrId;             // 모집자 ID

    @JacksonXmlProperty(localName = "saveDt")
    private String saveDt;               // 저장일

    /**
     * 외부 API 전체 응답 XML 매핑용 내부 스태틱 클래스
     */
    @Data
    @JacksonXmlRootElement(localName = "response")
    public static class ApiResponse {
        @JacksonXmlProperty(localName = "header")
        private Header header;

        @JacksonXmlProperty(localName = "body")
        private Body body;
    }

    @Data
    public static class Header {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;
    }

    @Data
    public static class Body {
        @JacksonXmlProperty(localName = "items")
        private Items items;

        @JacksonXmlProperty(localName = "numOfRows")
        private int numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private int pageNo;

        @JacksonXmlProperty(localName = "totalCount")
        private int totalCount;
    }

    @Data
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<DonationProgramDto> item;
    }
}

