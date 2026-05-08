package com.sism.main.interfaces.rest;

import com.sism.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("System Health Controller Tests")
class SystemHealthControllerTest {

    @Test
    @DisplayName("Should return up when application is live and ready")
    void shouldReturnUpWhenApplicationIsLiveAndReady() {
        ApplicationAvailability availability = mock(ApplicationAvailability.class);
        when(availability.getLivenessState()).thenReturn(LivenessState.CORRECT);
        when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
        SystemHealthController controller = new SystemHealthController(availability);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().getData().get("status"));
    }

    @Test
    @DisplayName("Should return down when application is not ready")
    void shouldReturnDownWhenApplicationIsNotReady() {
        ApplicationAvailability availability = mock(ApplicationAvailability.class);
        when(availability.getLivenessState()).thenReturn(LivenessState.BROKEN);
        when(availability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);
        SystemHealthController controller = new SystemHealthController(availability);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("DOWN", response.getBody().getData().get("status"));
    }
}
