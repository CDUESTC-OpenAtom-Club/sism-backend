package com.sism.controller;

import com.sism.config.TestSecurityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.dto.ApprovalRequest;
import com.sism.dto.LoginRequest;
import com.sism.dto.ReportCreateRequest;
import com.sism.dto.ReportUpdateRequest;
import com.sism.entity.*;
import com.sism.enums.ApprovalAction;
import com.sism.enums.MilestoneStatus;
import com.sism.enums.ReportStatus;
import com.sism.repository.*;
import com.sism.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReportController
 * Tests progress report CRUD, submit, withdraw, and approval endpoints
 * 
 * Requirements: 4.3 - Controller layer integration test coverage
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private SysUser testUser;
    private Indicator testIndicator;
    private Milestone testMilestone;
    private ProgressReport testReport;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data using TestDataFactory
        testIndicator = TestDataFactory.createTestIndicator(indicatorRepository, taskRepository, 
                                                            cycleRepository, orgRepository);
        testMilestone = TestDataFactory.createTestMilestone(milestoneRepository, indicatorRepository, 
                                                            taskRepository, cycleRepository, orgRepository);
        
        // Create test user with encoded password
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            SysUser user = new SysUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode("testPassword123"));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(testIndicator.getOwnerOrg());
            return userRepository.save(user);
        });

        // Create test report
        testReport = TestDataFactory.createTestReport(reportRepository, indicatorRepository, 
                                                     userRepository, taskRepository, cycleRepository, orgRepository);

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/reports/{id}")
    class GetReportByIdTests {

        @Test
        @DisplayName("Should return report by ID")
        void shouldReturnReportById() throws Exception {
            mockMvc.perform(get("/reports/{id}", testReport.getReportId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.reportId").value(testReport.getReportId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent report")
        void shouldReturn404ForNonExistentReport() throws Exception {
            mockMvc.perform(get("/reports/{id}", 999999L)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/reports/indicator/{indicatorId}")
    class GetReportsByIndicatorIdTests {

        @Test
        @DisplayName("Should return reports by indicator ID")
        void shouldReturnReportsByIndicatorId() throws Exception {
            mockMvc.perform(get("/reports/indicator/{indicatorId}", testIndicator.getIndicatorId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/reports/status/{status}")
    class GetReportsByStatusTests {

        @Test
        @DisplayName("Should return reports by status")
        void shouldReturnReportsByStatus() throws Exception {
            mockMvc.perform(get("/reports/status/{status}", ReportStatus.DRAFT)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/reports/pending-approval")
    class GetPendingApprovalReportsTests {

        @Test
        @DisplayName("Should return pending approval reports")
        void shouldReturnPendingApprovalReports() throws Exception {
            mockMvc.perform(get("/reports/pending-approval")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/reports")
    class CreateReportTests {

        @Test
        @DisplayName("Should create new report")
        void shouldCreateNewReport() throws Exception {
            ReportCreateRequest request = new ReportCreateRequest();
            request.setIndicatorId(testIndicator.getIndicatorId());
            request.setMilestoneId(testMilestone.getMilestoneId());
            request.setReporterId(testUser.getId());
            request.setNarrative("New progress report");
            request.setPercentComplete(BigDecimal.valueOf(30));

            mockMvc.perform(post("/reports")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.narrative").value("New progress report"));
        }
    }

    @Nested
    @DisplayName("PUT /api/reports/{id}")
    class UpdateReportTests {

        @Test
        @DisplayName("Should update draft report")
        void shouldUpdateDraftReport() throws Exception {
            ReportUpdateRequest request = new ReportUpdateRequest();
            request.setNarrative("Updated progress description");
            request.setPercentComplete(BigDecimal.valueOf(60));

            mockMvc.perform(put("/reports/{id}", testReport.getReportId())
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.narrative").value("Updated progress description"));
        }
    }

    @Nested
    @DisplayName("POST /api/reports/{id}/submit")
    class SubmitReportTests {

        @Test
        @DisplayName("Should submit draft report")
        void shouldSubmitDraftReport() throws Exception {
            mockMvc.perform(post("/reports/{id}/submit", testReport.getReportId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(ReportStatus.SUBMITTED.name()));
        }
    }

    @Nested
    @DisplayName("POST /api/reports/{id}/withdraw")
    class WithdrawReportTests {

        @Test
        @DisplayName("Should withdraw submitted report")
        void shouldWithdrawSubmittedReport() throws Exception {
            // First submit the report
            testReport.setStatus(ReportStatus.SUBMITTED);
            reportRepository.save(testReport);

            mockMvc.perform(post("/reports/{id}/withdraw", testReport.getReportId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(ReportStatus.DRAFT.name()));
        }
    }

    @Nested
    @DisplayName("GET /api/reports/{id}/approval-records")
    class GetApprovalRecordsTests {

        @Test
        @DisplayName("Should return approval records for report")
        void shouldReturnApprovalRecords() throws Exception {
            mockMvc.perform(get("/reports/{id}/approval-records", testReport.getReportId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/reports/approve")
    class ProcessApprovalTests {

        @Test
        @DisplayName("Should approve submitted report")
        void shouldApproveSubmittedReport() throws Exception {
            // First submit the report
            testReport.setStatus(ReportStatus.SUBMITTED);
            reportRepository.save(testReport);

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(testReport.getReportId());
            request.setAction(ApprovalAction.APPROVE);
            request.setComment("Approved");

            mockMvc.perform(post("/reports/approve")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(ReportStatus.APPROVED.name()));
        }

        @Test
        @DisplayName("Should reject submitted report")
        void shouldRejectSubmittedReport() throws Exception {
            // Create a new report for rejection test
            ProgressReport reportToReject = new ProgressReport();
            reportToReject.setIndicator(testIndicator);
            reportToReject.setMilestone(testMilestone);
            reportToReject.setReporter(testUser);
            reportToReject.setNarrative("Report to reject");
            reportToReject.setPercentComplete(BigDecimal.valueOf(40));
            reportToReject.setStatus(ReportStatus.SUBMITTED);
            reportToReject.setVersionNo(1);
            reportToReject.setIsFinal(false);
            reportToReject = reportRepository.save(reportToReject);

            ApprovalRequest request = new ApprovalRequest();
            request.setReportId(reportToReject.getReportId());
            request.setAction(ApprovalAction.REJECT);
            request.setComment("Rejected - needs more details");

            mockMvc.perform(post("/reports/approve")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value(ReportStatus.REJECTED.name()));
        }
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        return response.get("data").get("token").asText();
    }
}
