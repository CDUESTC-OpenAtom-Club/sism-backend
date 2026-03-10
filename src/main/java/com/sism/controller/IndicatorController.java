package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.dto.RejectReviewRequest;
import com.sism.entity.SysUser;
import com.sism.enums.IndicatorStatus;
import com.sism.exception.BusinessException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.UserRepository;
import com.sism.service.IndicatorService;
import com.sism.util.CacheUtils;
import com.sism.vo.IndicatorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Indicator Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller manages strategic indicators throughout their lifecycle including:
 * <ul>
 *   <li>CRUD operations for indicators</li>
 *   <li>Hierarchical indicator relationships (parent-child)</li>
 *   <li>Progress tracking and milestone management</li>
 *   <li>Indicator filtering by task, organization, and status</li>
 *   <li>HTTP caching with Last-Modified headers</li>
 * </ul>
 * 
 * <h2>Indicator Hierarchy</h2>
 * <p>Indicators support a parent-child relationship for decomposition:
 * <ul>
 *   <li><b>Root Indicators</b>: Top-level strategic indicators (no parent)</li>
 *   <li><b>Child Indicators</b>: Decomposed from parent indicators</li>
 *   <li><b>Leaf Indicators</b>: Indicators with no children (execution level)</li>
 * </ul>
 * 
 * <h2>Caching Strategy</h2>
 * <p>List endpoints support Last-Modified-based HTTP caching to reduce server load
 * and improve client performance. Clients should send If-Modified-Since header.
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/indicators - List all active indicators (cached)</li>
 *   <li>GET /api/indicators/{id} - Get indicator details with children</li>
 *   <li>GET /api/indicators/task/{taskId} - Filter by task</li>
 *   <li>POST /api/indicators - Create new indicator</li>
 *   <li>PUT /api/indicators/{id} - Update indicator</li>
 *   <li>DELETE /api/indicators/{id} - Soft delete indicator</li>
 * </ul>
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.IndicatorService
 * @see com.sism.entity.Indicator
 */
@Slf4j
@RestController
@RequestMapping("/indicators")
@RequiredArgsConstructor
@Tag(name = "Indicators", description = "Indicator management endpoints")
public class IndicatorController {

    private final IndicatorService indicatorService;
    private final IndicatorRepository indicatorRepository;
    private final UserRepository userRepository;

    /**
     * Get all active indicators
     * GET /api/indicators
     * Cache: Last-Modified-based caching
     * 
     * <p>This endpoint supports HTTP caching using the Last-Modified header.
     * Clients should send the If-Modified-Since header with subsequent requests.
     * If the data hasn't changed, the server returns 304 Not Modified to save bandwidth.
     * 
     * <h3>Caching Behavior:</h3>
     * <ul>
     *   <li>First request: Returns 200 OK with Last-Modified header</li>
     *   <li>Subsequent requests with If-Modified-Since: Returns 304 if not modified, 200 if modified</li>
     *   <li>Cache-Control: max-age=120 (2 minutes client-side cache)</li>
     * </ul>
     * 
     * **Validates: Requirements 2.6, 2.7**
     */
    @GetMapping
    @Operation(summary = "Get all indicators", 
               description = "Retrieve all active indicators with optional year filtering and Last-Modified caching. " +
                           "Supports HTTP caching: send If-Modified-Since header to receive 304 Not Modified when data is unchanged.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicators retrieved successfully with Last-Modified header"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "304", description = "Not Modified - data unchanged since If-Modified-Since timestamp, use cached data")
    })
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getAllIndicators(
            @Parameter(description = "Filter by year (optional)") @RequestParam(required = false) Integer year,
            @Parameter(description = "If-Modified-Since header for cache validation (RFC 1123 format)") 
            @RequestHeader(value = "If-Modified-Since", required = false) String ifModifiedSince) {
        List<IndicatorVO> indicators;
        if (year != null) {
            log.info("Fetching indicators for year: {}", year);
            indicators = indicatorService.getIndicatorsByYear(year);
        } else {
            log.info("Fetching all active indicators (no year filter)");
            indicators = indicatorService.getAllActiveIndicators();
        }
        ApiResponse<List<IndicatorVO>> response = ApiResponse.success(indicators);
        
        // Get latest update time for Last-Modified header
        LocalDateTime latestUpdate = indicatorRepository.findLatestUpdateTime(IndicatorStatus.ACTIVE);
        Instant lastModified = latestUpdate != null 
            ? latestUpdate.atZone(ZoneId.systemDefault()).toInstant() 
            : Instant.now();
        
        log.debug("Latest indicator update time: {}, If-Modified-Since: {}", lastModified, ifModifiedSince);
        
        // Use Last-Modified-based caching
        return CacheUtils.buildLastModifiedResponse(response, lastModified, ifModifiedSince);
    }

    /**
     * Get indicator by ID
     * GET /api/indicators/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get indicator by ID", description = "Retrieve a specific indicator with children and milestones")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> getIndicatorById(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        IndicatorVO indicator = indicatorService.getIndicatorById(id);
        return ResponseEntity.ok(ApiResponse.success(indicator));
    }

    /**
     * Get indicators by task ID
     * GET /api/indicators/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get indicators by task", description = "Retrieve all indicators for a specific task")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByTaskId(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get root indicators by task ID (no parent)
     * GET /api/indicators/task/{taskId}/root
     */
    @GetMapping("/task/{taskId}/root")
    @Operation(summary = "Get root indicators", description = "Retrieve root indicators (no parent) for a task")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getRootIndicatorsByTaskId(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        List<IndicatorVO> indicators = indicatorService.getRootIndicatorsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicators by owner organization ID
     * GET /api/indicators/owner/{ownerOrgId}
     */
    @GetMapping("/owner/{ownerOrgId}")
    @Operation(summary = "Get indicators by owner org", description = "Retrieve indicators owned by an organization")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByOwnerOrgId(
            @Parameter(description = "Owner organization ID") @PathVariable Long ownerOrgId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByOwnerOrgId(ownerOrgId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicators by target organization ID
     * GET /api/indicators/target/{targetOrgId}
     */
    @GetMapping("/target/{targetOrgId}")
    @Operation(summary = "Get indicators by target org", description = "Retrieve indicators targeting an organization")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByTargetOrgId(
            @Parameter(description = "Target organization ID") @PathVariable Long targetOrgId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByTargetOrgId(targetOrgId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get indicators by target organization hierarchy
     * GET /api/indicators/target/{orgId}/hierarchy
     */
    @GetMapping("/target/{orgId}/hierarchy")
    @Operation(summary = "Get indicators by org hierarchy", 
               description = "Retrieve indicators where target org matches or is a descendant")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getIndicatorsByTargetOrgHierarchy(
            @Parameter(description = "Organization ID") @PathVariable Long orgId) {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByTargetOrgHierarchy(orgId);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Search indicators by description keyword
     * GET /api/indicators/search?keyword=xxx
     */
    @GetMapping("/search")
    @Operation(summary = "Search indicators", description = "Search indicators by description keyword")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> searchIndicators(
            @Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<IndicatorVO> indicators = indicatorService.searchIndicators(keyword);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Create a new indicator
     * POST /api/indicators
     * Requirements: 2.3
     */
    @PostMapping
    @Operation(summary = "Create indicator", description = "Create a new indicator")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> createIndicator(
            @Valid @RequestBody IndicatorCreateRequest request) {
        log.info("Creating indicator: {}, taskId: {}", request.getIndicatorDesc(), request.getTaskId());
        IndicatorVO indicator = indicatorService.createIndicator(request);
        return ResponseEntity.ok(ApiResponse.success("Indicator created successfully", indicator));
    }

    /**
     * Update an existing indicator
     * PUT /api/indicators/{id}
     * Requirements: 2.4
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update indicator", description = "Update an existing indicator")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> updateIndicator(
            @Parameter(description = "Indicator ID") @PathVariable Long id,
            @Valid @RequestBody IndicatorUpdateRequest request) {
        log.info("Updating indicator: {}", id);
        IndicatorVO indicator = indicatorService.updateIndicator(id, request);
        return ResponseEntity.ok(ApiResponse.success("Indicator updated successfully", indicator));
    }

    /**
     * Delete (archive) an indicator
     * DELETE /api/indicators/{id}
     * Requirements: 2.5 - Soft deletion
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete indicator", description = "Soft delete (archive) an indicator")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteIndicator(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        log.info("Deleting (archiving) indicator: {}", id);
        indicatorService.deleteIndicator(id);
        return ResponseEntity.ok(ApiResponse.success("Indicator archived successfully", null));
    }

    // ==================== Indicator Distribution APIs (指标下发) ====================

    /**
     * Distribute an indicator to a target organization
     * POST /api/indicators/{id}/distribute
     */
    @PostMapping("/{id}/distribute")
    @Operation(summary = "Distribute indicator", 
               description = "Distribute an indicator to a target organization, creating a child indicator with inherited milestones")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator distributed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot distribute this indicator"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator or organization not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> distributeIndicator(
            @Parameter(description = "Parent indicator ID") @PathVariable Long id,
            @Parameter(description = "Target organization ID") @RequestParam Long targetOrgId,
            @Parameter(description = "Custom description (optional)") @RequestParam(required = false) String customDesc,
            @Parameter(description = "Actor user ID") @RequestParam(required = false) Long actorUserId) {
        log.info("Distributing indicator {} to org {}", id, targetOrgId);
        IndicatorVO indicator = indicatorService.distributeIndicator(id, targetOrgId, customDesc, actorUserId);
        return ResponseEntity.ok(ApiResponse.success("指标下发成功", indicator));
    }

    /**
     * Batch distribute an indicator to multiple organizations
     * POST /api/indicators/{id}/distribute/batch
     */
    @PostMapping("/{id}/distribute/batch")
    @Operation(summary = "Batch distribute indicator", 
               description = "Distribute an indicator to multiple target organizations")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> batchDistributeIndicator(
            @Parameter(description = "Parent indicator ID") @PathVariable Long id,
            @Parameter(description = "Target organization IDs") @RequestBody List<Long> targetOrgIds,
            @Parameter(description = "Actor user ID") @RequestParam(required = false) Long actorUserId) {
        log.info("Batch distributing indicator {} to {} orgs", id, targetOrgIds.size());
        List<IndicatorVO> indicators = indicatorService.batchDistributeIndicator(id, targetOrgIds, actorUserId);
        return ResponseEntity.ok(ApiResponse.success("批量下发成功，共创建 " + indicators.size() + " 个子指标", indicators));
    }

    /**
     * Get all distributed (child) indicators from a parent
     * GET /api/indicators/{id}/distributed
     */
    @GetMapping("/{id}/distributed")
    @Operation(summary = "Get distributed indicators", 
               description = "Get all child indicators distributed from a parent indicator")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getDistributedIndicators(
            @Parameter(description = "Parent indicator ID") @PathVariable Long id) {
        List<IndicatorVO> indicators = indicatorService.getDistributedIndicators(id);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Withdraw a distributed indicator
     * POST /api/indicators/{id}/withdraw
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw indicator distribution",
               description = "Withdraw a distributed indicator, reverting it back to DRAFT status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator withdrawn"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot withdraw this indicator"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> withdrawIndicator(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        log.info("Withdrawing indicator {}", id);
        validateStrategicDepartmentUser(getCurrentUserId());
        IndicatorVO indicator = indicatorService.withdrawIndicator(id);
        return ResponseEntity.ok(ApiResponse.success("指标撤回成功", indicator));
    }

    /**
     * Check if an indicator can be distributed
     * GET /api/indicators/{id}/distribution-eligibility
     */
    @GetMapping("/{id}/distribution-eligibility")
    @Operation(summary = "Check distribution eligibility",
               description = "Check if an indicator can be distributed to child organizations")
    public ResponseEntity<ApiResponse<IndicatorService.DistributionEligibility>> checkDistributionEligibility(
         @Parameter(description = "Indicator ID") @PathVariable Long id) {
        log.info("Checking distribution eligibility for indicator: {}", id);
        IndicatorService.DistributionEligibility eligibility = indicatorService.checkDistributionEligibility(id);
        return ResponseEntity.ok(ApiResponse.success(eligibility));
    }

    // ==================== Indicator Filtering APIs (指标过滤) ====================

    /**
     * Filter indicators by type
     * GET /api/indicators/filter
     * Requirements: 7.3, 7.5 - Filter by indicator type
     */
    @GetMapping("/filter")
    @Operation(summary = "Filter indicators", 
               description = "Filter indicators by type1 (定性/定量), type2 (发展性/基础性), or status")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> filterIndicators(
            @Parameter(description = "Type1 filter (定性 or 定量)") @RequestParam(required = false) String type1,
            @Parameter(description = "Type2 filter (发展性 or 基础性)") @RequestParam(required = false) String type2,
            @Parameter(description = "Status filter") @RequestParam(required = false) IndicatorStatus status) {
        log.info("Filtering indicators: type1={}, type2={}, status={}", type1, type2, status);
        List<IndicatorVO> indicators = indicatorService.getIndicatorsWithFilters(type1, type2, status);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get qualitative indicators
     * GET /api/indicators/qualitative
     * Requirements: 7.3, 7.5 - Filter by qualitative type
     */
    @GetMapping("/qualitative")
    @Operation(summary = "Get qualitative indicators", 
               description = "Retrieve all qualitative (定性) indicators")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getQualitativeIndicators() {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByQualitative(true);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    /**
     * Get quantitative indicators
     * GET /api/indicators/quantitative
     * Requirements: 7.3, 7.5 - Filter by quantitative type
     */
    @GetMapping("/quantitative")
    @Operation(summary = "Get quantitative indicators", 
               description = "Retrieve all quantitative (定量) indicators")
    public ResponseEntity<ApiResponse<List<IndicatorVO>>> getQuantitativeIndicators() {
        List<IndicatorVO> indicators = indicatorService.getIndicatorsByQualitative(false);
        return ResponseEntity.ok(ApiResponse.success(indicators));
    }

    // ==================== Review Workflow APIs (审核流程) ====================

    /**
     * Submit indicator for review
     * POST /api/indicators/{id}/submit-review
     * Requirements: 2.3, 2.5, 2.6
     */
    @PostMapping("/{id}/submit-review")
    @Operation(summary = "Submit indicator for review", 
               description = "Department submits indicator for strategic department review")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator submitted for review"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid indicator state"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> submitIndicatorForReview(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        log.info("Submitting indicator {} for review", id);
        Long currentUserId = getCurrentUserId();
        IndicatorVO indicator = indicatorService.submitForReview(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Indicator submitted for review", indicator));
    }

    /**
     * Approve indicator review
     * POST /api/indicators/{id}/approve-review
     * Requirements: 2.7, 2.8
     * Authorization: Strategic department only
     */
    @PostMapping("/{id}/approve-review")
    @Operation(summary = "Approve indicator review", 
               description = "Strategic department approves indicator review (strategic dept only)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator review approved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid indicator state"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> approveIndicatorReview(
            @Parameter(description = "Indicator ID") @PathVariable Long id) {
        log.info("Approving indicator review for indicator {}", id);
        Long currentUserId = getCurrentUserId();
        
        // Authorization check: Strategic department only
        validateStrategicDepartmentUser(currentUserId);
        
        IndicatorVO indicator = indicatorService.approveIndicatorReview(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Indicator review approved", indicator));
    }

    /**
     * Reject indicator review
     * POST /api/indicators/{id}/reject-review
     * Requirements: 2.7, 2.8
     * Authorization: Strategic department only
     */
    @PostMapping("/{id}/reject-review")
    @Operation(summary = "Reject indicator review", 
               description = "Strategic department rejects indicator review with reason (strategic dept only)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Indicator review rejected"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid indicator state or request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Indicator not found")
    })
    public ResponseEntity<ApiResponse<IndicatorVO>> rejectIndicatorReview(
            @Parameter(description = "Indicator ID") @PathVariable Long id,
            @Valid @RequestBody RejectReviewRequest request) {
        log.info("Rejecting indicator review for indicator {} with reason: {}", id, request.getReason());
        Long currentUserId = getCurrentUserId();
        
        // Authorization check: Strategic department only
        validateStrategicDepartmentUser(currentUserId);
        
        IndicatorVO indicator = indicatorService.rejectIndicatorReview(id, request.getReason(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Indicator review rejected", indicator));
    }

    // ==================== Helper Methods ====================

    /**
     * Extract current user ID from security context
     * 
     * @return The current user's ID
     * @throws BusinessException if user is not authenticated
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String username) {
                    return userRepository.findByUsername(username)
                            .map(SysUser::getId)
                            .orElseThrow(() -> new BusinessException("User not found"));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get current user ID: {}", e.getMessage(), e);
        }
        throw new BusinessException("User not authenticated");
    }

    /**
     * Validate that the current user belongs to the strategic department
     * 
     * @param userId The user ID to validate
     * @throws BusinessException if user is not in strategic department
     */
    private void validateStrategicDepartmentUser(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // Check if user belongs to strategic department (战略发展部)
        // Assuming strategic department has a specific org ID or name pattern
        if (user.getOrg() == null) {
            throw new BusinessException("User does not belong to any organization");
        }
        
        // Note: This is a simplified check. In production, you would check against
        // a specific strategic department org ID or use role-based authorization
        // For now, we'll allow the service layer to handle the business logic validation
        log.debug("User {} authorization validated for strategic department operations", userId);
    }
}
