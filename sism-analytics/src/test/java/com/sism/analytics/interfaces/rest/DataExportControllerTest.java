package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DataExportApplicationService;
import com.sism.analytics.application.ExportService;
import com.sism.analytics.domain.DataExport;
import com.sism.analytics.interfaces.dto.CompleteDataExportRequest;
import com.sism.analytics.interfaces.dto.CreateDataExportRequest;
import com.sism.iam.application.dto.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DataExportController Tests")
class DataExportControllerTest {

    @Test
    @DisplayName("createDataExport should ignore spoofed requestedBy and use authenticated user")
    void createDataExportShouldIgnoreSpoofedRequestedBy() {
        DataExportApplicationService service = mock(DataExportApplicationService.class);
        ExportService exportService = mock(ExportService.class);
        DataExportController controller = new DataExportController(service, exportService);

        CreateDataExportRequest request = new CreateDataExportRequest();
        request.setName("导出任务");
        request.setType("INDICATOR_DATA");
        request.setFormat(DataExport.FORMAT_EXCEL);
        request.setRequestedBy(2L);

        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        when(service.createDataExport("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null))
                .thenReturn(DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null));

        assertDoesNotThrow(() -> controller.createDataExport(currentUser, request));
        verify(service).createDataExport("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null);
    }

    @Test
    @DisplayName("createDataExport should use authenticated user id")
    void createDataExportShouldUseAuthenticatedUserId() {
        DataExportApplicationService service = mock(DataExportApplicationService.class);
        ExportService exportService = mock(ExportService.class);
        DataExportController controller = new DataExportController(service, exportService);

        DataExport dataExport = DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null);
        when(service.createDataExport("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, "{\"foo\":1}"))
                .thenReturn(dataExport);

        CreateDataExportRequest request = new CreateDataExportRequest();
        request.setName("导出任务");
        request.setType("INDICATOR_DATA");
        request.setFormat(DataExport.FORMAT_EXCEL);
        request.setRequestedBy(1L);
        request.setParameters("{\"foo\":1}");

        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertDoesNotThrow(() -> controller.createDataExport(currentUser, request));
        verify(service).createDataExport("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, "{\"foo\":1}");
    }

    @Test
    @DisplayName("user-scoped endpoints should reject other users")
    void userScopedEndpointsShouldRejectOtherUsers() {
        DataExportApplicationService service = mock(DataExportApplicationService.class);
        ExportService exportService = mock(ExportService.class);
        DataExportController controller = new DataExportController(service, exportService);
        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertThrows(AccessDeniedException.class, () -> controller.getDataExportsByRequestedBy(currentUser, 2L));
        assertThrows(AccessDeniedException.class, () -> controller.getDownloadableByRequestedBy(currentUser, 2L));
        assertThrows(AccessDeniedException.class, () -> controller.getDataExportsByRequestedByAndStatus(currentUser, 2L, "FAILED"));
        assertThrows(AccessDeniedException.class, () -> controller.countDataExportsByRequestedBy(currentUser, 2L));
        verifyNoInteractions(service, exportService);
    }

    @Test
    @DisplayName("all mapped endpoints should require authentication")
    void allMappedEndpointsShouldRequireAuthentication() {
        for (Method method : DataExportController.class.getDeclaredMethods()) {
            boolean mapped = method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
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
