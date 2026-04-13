package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.ReportApplicationService;
import com.sism.analytics.application.AnalyticsFileStorageService;
import com.sism.analytics.domain.Report;
import com.sism.analytics.interfaces.dto.CreateReportRequest;
import com.sism.analytics.interfaces.dto.GenerateReportRequest;
import com.sism.iam.application.dto.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ReportController Tests")
class ReportControllerTest {

    @Test
    @DisplayName("createReport should ignore spoofed generatedBy and use authenticated user")
    void createReportShouldIgnoreSpoofedGeneratedBy() {
        ReportApplicationService service = mock(ReportApplicationService.class);
        AnalyticsFileStorageService fileStorageService = mock(AnalyticsFileStorageService.class);
        ReportController controller = new ReportController(service, fileStorageService);

        CreateReportRequest request = new CreateReportRequest();
        request.setName("分析报告");
        request.setType(Report.TYPE_STRATEGIC);
        request.setFormat(Report.FORMAT_PDF);
        request.setParameters("{\"foo\":1}");
        request.setDescription("年度总结");

        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        when(service.createReport("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, "{\"foo\":1}", "年度总结"))
                .thenReturn(Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, "年度总结"));

        assertDoesNotThrow(() -> controller.createReport(currentUser, request));
        verify(service).createReport("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, "{\"foo\":1}", "年度总结");
    }

    @Test
    @DisplayName("createReport should use authenticated user id")
    void createReportShouldUseAuthenticatedUserId() {
        ReportApplicationService service = mock(ReportApplicationService.class);
        AnalyticsFileStorageService fileStorageService = mock(AnalyticsFileStorageService.class);
        ReportController controller = new ReportController(service, fileStorageService);

        Report report = Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        when(service.createReport("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, "{\"foo\":1}", "年度总结"))
                .thenReturn(report);

        CreateReportRequest request = new CreateReportRequest();
        request.setName("分析报告");
        request.setType(Report.TYPE_STRATEGIC);
        request.setFormat(Report.FORMAT_PDF);
        request.setParameters("{\"foo\":1}");
        request.setDescription("年度总结");

        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertDoesNotThrow(() -> controller.createReport(currentUser, request));
        verify(service).createReport("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, "{\"foo\":1}", "年度总结");
    }

    @Test
    @DisplayName("user-scoped endpoints should reject other users")
    void userScopedEndpointsShouldRejectOtherUsers() {
        ReportApplicationService service = mock(ReportApplicationService.class);
        AnalyticsFileStorageService fileStorageService = mock(AnalyticsFileStorageService.class);
        ReportController controller = new ReportController(service, fileStorageService);
        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertThrows(AccessDeniedException.class, () -> controller.getReportsByGeneratedBy(currentUser, 2L));
        assertThrows(AccessDeniedException.class, () -> controller.countReportsByGeneratedBy(currentUser, 2L));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("generateReport should use managed server-side file path")
    void generateReportShouldUseManagedServerSideFilePath() {
        ReportApplicationService service = mock(ReportApplicationService.class);
        AnalyticsFileStorageService fileStorageService = mock(AnalyticsFileStorageService.class);
        ReportController controller = new ReportController(service, fileStorageService);
        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());
        Report report = Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        report.setId(8L);

        when(service.findReportById(8L, 1L)).thenReturn(java.util.Optional.of(report));
        when(fileStorageService.prepareManagedReportFile(report))
                .thenReturn(java.nio.file.Path.of("/tmp/managed-report.pdf"));
        when(service.generateReport(8L, 1L, "/tmp/managed-report.pdf", 0L)).thenReturn(report);

        GenerateReportRequest request = new GenerateReportRequest();
        request.setFileSize(0L);

        assertDoesNotThrow(() -> controller.generateReport(currentUser, 8L, request));
        verify(service).generateReport(8L, 1L, "/tmp/managed-report.pdf", 0L);
    }

    @Test
    @DisplayName("all mapped endpoints should require authentication")
    void allMappedEndpointsShouldRequireAuthentication() {
        for (Method method : ReportController.class.getDeclaredMethods()) {
            boolean mapped = method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class);
            if (!mapped) {
                continue;
            }
            assertEquals(
                    true,
                    method.isAnnotationPresent(PreAuthorize.class),
                    "Missing @PreAuthorize on " + method.getName()
            );
        }
    }

    @Test
    @DisplayName("DTO should sanitize report file path and sensitive error details")
    void dtoShouldSanitizeReportSensitiveFields() {
        ReportApplicationService service = mock(ReportApplicationService.class);
        AnalyticsFileStorageService fileStorageService = mock(AnalyticsFileStorageService.class);
        ReportController controller = new ReportController(service, fileStorageService);

        Report generated = Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        generated.setId(8L);
        generated.generate("/srv/reports/secret/analysis.pdf", 1024L);
        when(service.findReportById(8L, 1L)).thenReturn(java.util.Optional.of(generated));

        var generatedResponse = controller.getReportById(new CurrentUser(1L, "alice", "Alice", null, 10L, List.of()), 8L);
        assertEquals("analysis.pdf", generatedResponse.getBody().getData().getFilePath());

        Report failed = Report.create("失败报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        failed.setId(9L);
        failed.fail("java.lang.RuntimeException: /srv/reports/secret/analysis.pdf");
        when(service.findReportById(9L, 1L)).thenReturn(java.util.Optional.of(failed));

        var failedResponse = controller.getReportById(new CurrentUser(1L, "alice", "Alice", null, 10L, List.of()), 9L);
        assertEquals("报告生成失败，请稍后重试或联系管理员", failedResponse.getBody().getData().getErrorMessage());
    }

}
