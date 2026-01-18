package com.sism.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private AppUser testUser;
    private Indicator testIndicator;
    private Milestone testMilestone;
    private ProgressReport testReport;

    @BeforeEach
    void setUp() throws Exception {
        // Get or create test user and login
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode("testPassword123"));
            user.setRealName("Test User");
            user.setIsActive(true);
            user.setOrg(orgRepository.findAll().stream().findFirst().orElseThrow());
            return userRepository.save(user);
        });

        // Get test indicator
        testIndicator = indicatorRepository.findAll().stream()
                .findFirst()
                .orElseThrow();

        // Get or create test milestone
        testMilestone = milestoneRepository.findByIndicator_IndicatorId(testIndicator.getIndicatorId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Milestone milestone = new Milestone();
                    milestone.setMilestoneName("Test Milestone for Report");
                    milestone.setIndicator(testIndicator);
                    milestone.setDueDate(LocalDate.now().plusMonths(1));
                    milestone.setWeightPercent(BigDecimal.valueOf(25));
                    milestone.setStatus(MilestoneStatus.NOT_STARTED);
                    return milestoneRepository.save(milestone);
                });

        // Get or create test report
        testReport = reportRepository.findByIndicator_IndicatorId(testIndicator.getIndicatorId())
                .stream()
                .filter(r -> r.getStatus() == ReportStatus.DRAFT)
                .findFirst()
                .orElseGet(() -> {
                    ProgressReport report = new ProgressReport();
                    report.setIndicator(testIndicator);
                    report.setMilestone(testMilestone);
                    report.setReporter(testUser);
                    report.setNarrative("Test progress description");
                    report.setPercentComplete(BigDecimal.valueOf(50));
                    report.setStatus(ReportStatus.DRAFT);
                    report.setVersionNo(1);
                    report.setIsFinal(false);
                    return reportRepository.save(report);
                });

        // Login to get token
        authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
    }

    @Nested
    @DisplayName("GET /api/reports/{id}")
    class GetReportByIdTests {

        @Test
        @DisplayName("Should return report by ID")
        void shouldReturnReportById() throws Exception {
            mockMvc.perform(get("/api/reports/{id}", testReport.getReportId())
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.reportId").value(testReport.getReportId()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent report")
        void shouldReturn404ForNonExistentReport() throws Exception {
            mockMvc.perform(get("/api/reports/{id}", 999999L)
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
            mockMvc.perform(get("/api/reports/indicator/{indicatorId}", testIndicator.getIndicatorId())
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
            mockMvc.perform(get("/api/reports/status/{status}", ReportStatus.DRAFT)
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
            mockMvc.perform(get("/api/reports/pending-approval")
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
            request.setReporterId(testUser.getUserId());
            request.setNarrative("New progress report");
            request.setPercentComplete(BigDecimal.valueOf(30));

            mockMvc.perform(post("/api/reports")
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

            mockMvc.perform(put("/api/reports/{id}", testReport.getReportId())
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
            mockMvc.perform(post("/api/reports/{id}/submit", testReport.getReportId())
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

            mockMvc.perform(post("/api/reports/{id}/withdraw", testReport.getReportId())
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
            mockMvc.perform(get("/api/reports/{id}/approval-records", testReport.getReportId())
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
            request.setApproverId(testUser.getUserId());
            request.setComment("Approved");

            mockMvc.perform(post("/api/reports/approve")
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
            request.setApproverId(testUser.getUserId());
            request.setComment("Rejected - needs more details");

            mockMvc.perform(post("/api/reports/approve")
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

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(responseBody);
        return response.get("data").get("token").asText();
    }
}
