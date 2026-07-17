package com.doumi.donation.api;

import com.doumi.donation.config.SecurityConfig;
import com.doumi.donation.publicdata.controller.AdminBatchController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminBatchController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("관리자 배치 /api/admin")
class AdminBatchControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean(name = "asyncJobLauncher")
    JobLauncher jobLauncher;
    @MockitoBean
    Job publicDataImportJob;
    @MockitoBean
    JobExplorer jobExplorer;

    @Test
    @DisplayName("POST /api/admin/public-data → 202 공공데이터 수집 배치 시작 (관리자)")
    void 배치실행() throws Exception {
        when(jobExplorer.findRunningJobExecutions("publicDataImportJob"))
                .thenReturn(Collections.emptySet());

        mvc.perform(post("/api/admin/public-data"))
                .andExpect(status().isAccepted());
    }
}
