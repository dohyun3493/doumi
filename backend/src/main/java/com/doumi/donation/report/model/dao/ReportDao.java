package com.doumi.donation.report.model.dao;

import com.doumi.donation.report.model.dto.CampaignReport;
import com.doumi.donation.report.model.dto.ReportExpense;
import com.doumi.donation.report.model.dto.ReportImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportDao {
    void insertReport(CampaignReport report);    // useGeneratedKeys로 reportId 채워짐
    void insertExpense(@Param("reportId") long reportId,
                       @Param("item") String item,
                       @Param("amount") long amount);
    void insertImage(@Param("reportId") long reportId,
                     @Param("imageUrl") String imageUrl,
                     @Param("imageType") String imageType);
    CampaignReport findReportByCampaignId(@Param("campaignId") long campaignId);
    List<ReportExpense> findExpensesByReportId(@Param("reportId") long reportId);
    List<ReportImage> findImagesByReportId(@Param("reportId") long reportId);
    boolean existsByCampaignId(@Param("campaignId") long campaignId);

    // 수정용: 본문 갱신 + 지출/이미지는 전부 지우고 다시 삽입(replace-all)
    void updateReportContent(@Param("reportId") long reportId, @Param("content") String content);
    void deleteExpensesByReportId(@Param("reportId") long reportId);
    void deleteImagesByReportId(@Param("reportId") long reportId);
}
