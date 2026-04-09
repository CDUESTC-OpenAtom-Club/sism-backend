package com.sism.analytics.application;

import com.sism.analytics.domain.DataExport;
import com.sism.analytics.infrastructure.repository.DataExportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService Tests")
class ExportServiceTest {

    @Mock
    private DataExportApplicationService dataExportApplicationService;

    @Mock
    private DataExportRepository dataExportRepository;

    @TempDir
    Path tempDir;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(dataExportApplicationService, dataExportRepository);
        ReflectionTestUtils.setField(exportService, "exportBasePath", tempDir.toString());
    }

    @Test
    @DisplayName("getExportFilePath should reject access to another user's export")
    void getExportFilePathShouldRejectOtherUsersExport() {
        DataExport export = DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 2L, null);
        export.setId(10L);
        export.startProcessing();
        export.complete(tempDir.resolve("other-user.xlsx").toString(), 128L);

        when(dataExportApplicationService.findDataExportById(10L)).thenReturn(Optional.of(export));

        assertThrows(AccessDeniedException.class, () -> exportService.getExportFilePath(10L, 1L));
        verify(dataExportApplicationService).findDataExportById(10L);
    }

    @Test
    @DisplayName("getExportFilePath should return the owned downloadable file path")
    void getExportFilePathShouldReturnOwnedDownloadableFilePath() {
        Path filePath = tempDir.resolve("owned.xlsx");
        assertDoesNotThrow(() -> java.nio.file.Files.writeString(filePath, "ok"));
        DataExport export = DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null);
        export.setId(11L);
        export.startProcessing();
        export.complete(filePath.toString(), 256L);

        when(dataExportApplicationService.findDataExportById(11L)).thenReturn(Optional.of(export));

        assertEquals(filePath, exportService.getExportFilePath(11L, 1L));
    }

    @Test
    @DisplayName("getExportFilePath should reject paths outside configured export directory")
    void getExportFilePathShouldRejectPathTraversal() {
        Path outsideFile = tempDir.getParent().resolve("escape.xlsx");
        assertDoesNotThrow(() -> java.nio.file.Files.writeString(outsideFile, "bad"));

        DataExport export = DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null);
        export.setId(12L);
        export.startProcessing();
        export.complete(outsideFile.toString(), 10L);

        when(dataExportApplicationService.findDataExportById(12L)).thenReturn(Optional.of(export));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exportService.getExportFilePath(12L, 1L)
        );

        assertTrue(exception.getMessage().contains("escapes configured export directory"));
    }

    @Test
    @DisplayName("exportToExcel should reject blank export name")
    void exportToExcelShouldRejectBlankExportName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                exportService.exportToExcel(" ", "INDICATOR_DATA", 1L, java.util.List.of(), java.util.List.of("name"))
        );

        assertTrue(exception.getMessage().contains("Export name cannot be null or blank"));
        verifyNoInteractions(dataExportApplicationService, dataExportRepository);
    }

    @Test
    @DisplayName("exportToCSV should reject empty headers")
    void exportToCSVShouldRejectEmptyHeaders() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                exportService.exportToCSV("导出", "INDICATOR_DATA", 1L, java.util.List.of(), java.util.List.of())
        );

        assertTrue(exception.getMessage().contains("Export headers cannot be null or empty"));
        verifyNoInteractions(dataExportApplicationService, dataExportRepository);
    }

    @Test
    @DisplayName("exportToExcel should reject null data")
    void exportToExcelShouldRejectNullData() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                exportService.exportToExcel("导出", "INDICATOR_DATA", 1L, null, java.util.List.of("name"))
        );

        assertTrue(exception.getMessage().contains("Export data cannot be null"));
        verifyNoInteractions(dataExportApplicationService, dataExportRepository);
    }
}
