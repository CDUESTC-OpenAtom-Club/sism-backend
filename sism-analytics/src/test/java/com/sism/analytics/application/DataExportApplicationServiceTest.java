package com.sism.analytics.application;

import com.sism.analytics.domain.DataExport;
import com.sism.analytics.infrastructure.repository.DataExportRepository;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataExportApplicationService Tests")
class DataExportApplicationServiceTest {

    @Mock
    private DataExportRepository dataExportRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    private DataExportApplicationService dataExportApplicationService;

    @BeforeEach
    void setUp() {
        dataExportApplicationService = new DataExportApplicationService(dataExportRepository, eventPublisher, eventStore);
    }

    @Test
    @DisplayName("startProcessing should reject non-owner access")
    void startProcessingShouldRejectNonOwnerAccess() {
        DataExport dataExport = DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null);
        dataExport.setId(99L);
        when(dataExportRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.of(dataExport));

        assertThrows(AccessDeniedException.class, () ->
                dataExportApplicationService.startProcessing(99L, 2L)
        );
        verify(dataExportRepository, never()).save(any(DataExport.class));
    }

    @Test
    @DisplayName("findDataExportById should hide other users export")
    void findDataExportByIdShouldHideOtherUsersExport() {
        DataExport dataExport = DataExport.create("导出任务", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null);
        dataExport.setId(100L);
        when(dataExportRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(dataExport));

        assertEquals(Optional.empty(), dataExportApplicationService.findDataExportById(100L, 2L));
    }

    @Test
    @DisplayName("findDataExportsByRequestedBy should reject other users request")
    void findDataExportsByRequestedByShouldRejectOtherUsersRequest() {
        assertThrows(AccessDeniedException.class, () ->
                dataExportApplicationService.findDataExportsByRequestedBy(2L, 1L)
        );
        verifyNoInteractions(dataExportRepository);
    }

    @Test
    @DisplayName("countDataExportsByRequestedBy should reject other users request")
    void countDataExportsByRequestedByShouldRejectOtherUsersRequest() {
        assertThrows(AccessDeniedException.class, () ->
                dataExportApplicationService.countDataExportsByRequestedBy(2L, 1L)
        );
        verifyNoInteractions(dataExportRepository);
    }

    @Test
    @DisplayName("findDataExportsByDateRange should reject invalid date range")
    void findDataExportsByDateRangeShouldRejectInvalidDateRange() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 6, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 5, 12, 0);

        assertThrows(IllegalArgumentException.class, () ->
                dataExportApplicationService.findDataExportsByDateRange(start, end, 1L)
        );
        verifyNoInteractions(dataExportRepository);
    }

    @Test
    @DisplayName("searchDataExportsByName should escape SQL LIKE wildcards")
    void searchDataExportsByNameShouldEscapeLikeWildcards() {
        dataExportApplicationService.searchDataExportsByName("100%_done", 1L);

        verify(dataExportRepository).findByRequestedByAndNameContainingAndNotDeleted(1L, "100\\%\\_done");
    }
}
