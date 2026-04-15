package com.sism.analytics.interfaces.rest;

import com.sism.analytics.application.DashboardApplicationService;
import com.sism.analytics.domain.Dashboard;
import com.sism.analytics.interfaces.dto.CreateDashboardRequest;
import com.sism.iam.application.dto.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@DisplayName("DashboardController Tests")
class DashboardControllerTest {

    @Test
    @DisplayName("createDashboard should ignore spoofed userId and use authenticated user")
    void createDashboardShouldIgnoreSpoofedUserId() {
        DashboardApplicationService service = mock(DashboardApplicationService.class);
        DashboardController controller = new DashboardController(service);

        CreateDashboardRequest request = new CreateDashboardRequest();
        request.setName("我的看板");
        request.setUserId(2L);
        request.setIsPublic(false);

        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        when(service.createDashboard("我的看板", null, 1L, false, null))
                .thenReturn(Dashboard.create("我的看板", null, 1L, false, null));

        assertDoesNotThrow(() -> controller.createDashboard(currentUser, request));
        verify(service).createDashboard("我的看板", null, 1L, false, null);
    }

    @Test
    @DisplayName("createDashboard should use authenticated user id")
    void createDashboardShouldUseAuthenticatedUserId() {
        DashboardApplicationService service = mock(DashboardApplicationService.class);
        DashboardController controller = new DashboardController(service);

        Dashboard dashboard = Dashboard.create("我的看板", "描述", 1L, false, "{}");
        when(service.createDashboard("我的看板", "描述", 1L, false, "{}")).thenReturn(dashboard);

        CreateDashboardRequest request = new CreateDashboardRequest();
        request.setName("我的看板");
        request.setDescription("描述");
        request.setUserId(1L);
        request.setIsPublic(false);
        request.setConfig("{}");

        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertDoesNotThrow(() -> controller.createDashboard(currentUser, request));
        verify(service).createDashboard("我的看板", "描述", 1L, false, "{}");
    }

    @Test
    @DisplayName("user-scoped endpoints should reject other users")
    void userScopedEndpointsShouldRejectOtherUsers() {
        DashboardApplicationService service = mock(DashboardApplicationService.class);
        DashboardController controller = new DashboardController(service);
        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());

        assertThrows(AccessDeniedException.class, () -> controller.getDashboardsByUserId(currentUser, 2L));
        assertThrows(AccessDeniedException.class, () -> controller.getPublicDashboardsByUserId(currentUser, 2L));
        assertThrows(AccessDeniedException.class, () -> controller.searchDashboardsByName(currentUser, 2L, "test"));
        assertThrows(AccessDeniedException.class, () -> controller.countDashboardsByUserId(currentUser, 2L));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("paging endpoints should delegate to application service")
    void pagingEndpointsShouldDelegateToApplicationService() {
        DashboardApplicationService service = mock(DashboardApplicationService.class);
        DashboardController controller = new DashboardController(service);
        CurrentUser currentUser = new CurrentUser(1L, "alice", "Alice", null, 10L, List.of());
        Dashboard dashboard = Dashboard.create("我的看板", "描述", 1L, false, "{}");

        when(service.findDashboardsByUserId(1L, 1L, 1, 20))
                .thenReturn(new PageImpl<>(List.of(dashboard), PageRequest.of(0, 20), 1));
        when(service.findPublicDashboardsByUserId(1L, 1L, 1, 20))
                .thenReturn(new PageImpl<>(List.of(dashboard), PageRequest.of(0, 20), 1));
        when(service.searchDashboardsByName(1L, 1L, "我的", 1, 20))
                .thenReturn(new PageImpl<>(List.of(dashboard), PageRequest.of(0, 20), 1));

        assertDoesNotThrow(() -> controller.getDashboardsByUserIdPage(currentUser, 1L, 1, 20));
        assertDoesNotThrow(() -> controller.getPublicDashboardsByUserIdPage(currentUser, 1L, 1, 20));
        assertDoesNotThrow(() -> controller.searchDashboardsByNamePage(currentUser, 1L, "我的", 1, 20));

        verify(service).findDashboardsByUserId(1L, 1L, 1, 20);
        verify(service).findPublicDashboardsByUserId(1L, 1L, 1, 20);
        verify(service).searchDashboardsByName(1L, 1L, "我的", 1, 20);
    }

    @Test
    @DisplayName("all mapped endpoints should require authentication")
    void allMappedEndpointsShouldRequireAuthentication() {
        for (Method method : DashboardController.class.getDeclaredMethods()) {
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
