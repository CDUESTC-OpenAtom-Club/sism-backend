package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.ReportApplicationService;
import com.sism.analytics.domain.Report;
import com.sism.analytics.interfaces.dto.CreateReportRequest;
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
        ReportController controller = new ReportController(service);

        CreateReportRequest request = new CreateReportRequest();
        request.setName("分析报告");
        request.setType(Report.TYPE_STRATEGIC);
        request.setFormat(Report.FORMAT_PDF);
        request.setGeneratedBy(2L);
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
        ReportController controller = new ReportController(service);

        Report report = Report.create("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null);
        when(service.createReport("分析报告", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, "{\"foo\":1}", "年度总结"))
                .thenReturn(report);

        CreateReportRequest request = new CreateReportRequest();
        request.setName("分析报告");
        request.setType(Report.TYPE_STRATEGIC);
        request.setFormat(Report.FORMAT_PDF);
        request.setGeneratedBy(1L);
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
        ReportController controller = new ReportController(service);
        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertThrows(AccessDeniedException.class, () -> controller.getReportsByGeneratedBy(currentUser, 2L));
        assertThrows(AccessDeniedException.class, () -> controller.countReportsByGeneratedBy(currentUser, 2L));
        verifyNoInteractions(service);
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

}
