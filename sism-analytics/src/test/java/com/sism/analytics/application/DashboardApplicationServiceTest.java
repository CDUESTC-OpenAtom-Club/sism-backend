package com.sism.analytics.application;

import com.sism.analytics.domain.Dashboard;
import com.sism.analytics.infrastructure.repository.DashboardRepository;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardApplicationService Tests")
class DashboardApplicationServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private EventStore eventStore;

    private DashboardApplicationService dashboardApplicationService;

    @BeforeEach
    void setUp() {
        dashboardApplicationService = new DashboardApplicationService(dashboardRepository, eventPublisher, eventStore);
    }

    @Test
    @DisplayName("updateDashboard should reject non-owner access")
    void updateDashboardShouldRejectNonOwnerAccess() {
        Dashboard dashboard = Dashboard.create("团队看板", "描述", 1L, false, "{}");
        dashboard.setId(99L);
        when(dashboardRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.of(dashboard));

        assertThrows(AccessDeniedException.class, () ->
                dashboardApplicationService.updateDashboard(99L, 2L, "新名字", "新描述", true, "{\"widgets\":[]}")
        );
        verify(dashboardRepository, never()).save(any(Dashboard.class));
    }

    @Test
    @DisplayName("deleteDashboard should reject non-owner access")
    void deleteDashboardShouldRejectNonOwnerAccess() {
        Dashboard dashboard = Dashboard.create("团队看板", "描述", 1L, false, "{}");
        dashboard.setId(100L);
        when(dashboardRepository.findByIdAndNotDeleted(100L)).thenReturn(Optional.of(dashboard));

        assertThrows(AccessDeniedException.class, () -> dashboardApplicationService.deleteDashboard(100L, 2L));
        verify(dashboardRepository, never()).save(any(Dashboard.class));
    }

    @Test
    @DisplayName("deleteDashboard should publish domain event after state change")
    void deleteDashboardShouldPublishDomainEventAfterStateChange() {
        Dashboard dashboard = Dashboard.create("团队看板", "描述", 1L, false, "{}");
        dashboard.setId(101L);
        when(dashboardRepository.findByIdAndNotDeleted(101L)).thenReturn(Optional.of(dashboard));

        assertDoesNotThrow(() -> dashboardApplicationService.deleteDashboard(101L, 1L));

        verify(eventStore).save(any());
        verify(dashboardRepository).save(dashboard);
    }

    @Test
    @DisplayName("copyDashboardToUser should reject invalid target user id")
    void copyDashboardToUserShouldRejectInvalidTargetUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                dashboardApplicationService.copyDashboardToUser(101L, 1L, 0L)
        );
        verify(dashboardRepository, never()).save(any(Dashboard.class));
    }

    @Test
    @DisplayName("findDashboardsByUserId should reject other user's request")
    void findDashboardsByUserIdShouldRejectOtherUsersRequest() {
        assertThrows(AccessDeniedException.class, () ->
                dashboardApplicationService.findDashboardsByUserId(2L, 1L)
        );
        verifyNoInteractions(dashboardRepository);
    }

    @Test
    @DisplayName("findDashboardById should hide private dashboards from other users")
    void findDashboardByIdShouldHidePrivateDashboardsFromOtherUsers() {
        Dashboard dashboard = Dashboard.create("私有看板", "描述", 1L, false, "{}");
        dashboard.setId(102L);
        when(dashboardRepository.findByIdAndNotDeleted(102L)).thenReturn(Optional.of(dashboard));

        assertEquals(Optional.empty(), dashboardApplicationService.findDashboardById(102L, 2L));
    }

    @Test
    @DisplayName("createDashboard should persist dashboard for valid user")
    void createDashboardShouldPersistDashboardForValidUser() {
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dashboard dashboard = assertDoesNotThrow(() ->
                dashboardApplicationService.createDashboard("有效看板", "描述", 1L, false, "{}")
        );

        assertEquals("有效看板", dashboard.getName());
        verify(dashboardRepository).save(any(Dashboard.class));
    }

    @Test
    @DisplayName("searchDashboardsByName should escape SQL LIKE wildcards")
    void searchDashboardsByNameShouldEscapeLikeWildcards() {
        dashboardApplicationService.searchDashboardsByName(1L, 1L, "100%_done");

        verify(dashboardRepository).findByUserIdAndNameContainingAndNotDeleted(1L, "100\\%\\_done");
    }
}
