package com.sism.controller;

import com.sism.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides system health status for frontend monitoring
 */
@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "System health check endpoints")
public class HealthController {

    /**
     * Health check endpoint
     * GET /api/health
     * Returns system status and timestamp
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if the backend system is running normally")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "sism-backend");
        health.put("version", "1.0.0");
        
        log.debug("Health check performed");
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
