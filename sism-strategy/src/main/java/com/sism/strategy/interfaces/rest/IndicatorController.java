package com.sism.strategy.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.common.PageResult;
import com.sism.strategy.domain.enums.IndicatorStatus;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.repository.OrganizationRepository;
import com.sism.strategy.application.MilestoneApplicationService;
import com.sism.strategy.application.StrategyApplicationService;
import com.sism.strategy.domain.Indicator;
import com.sism.task.domain.repository.TaskRepository;
import com.sism.task.infrastructure.persistence.JpaTaskRepositoryInternal;
import com.sism.strategy.interfaces.dto.BatchDistributeIndicatorsRequest;
import com.sism.strategy.interfaces.dto.BatchDistributeIndicatorsResponse;
import com.sism.strategy.interfaces.dto.MilestoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/indicators")
@RequiredArgsConstructor
@Tag(name = "指标管理", description = "指标管理相关接口")
public class IndicatorController {

    private record CurrentMonthIndicatorRoundState(Integer progress, boolean hasCurrentMonthFill) {}

    private static String currentReportMonth() {
        return java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private final StrategyApplicationService strategyApplicationService;
    private final MilestoneApplicationService milestoneApplicationService;
    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;
    private final JpaTaskRepositoryInternal jpaTaskRepository;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "分页获取所有指标")
    public ResponseEntity<ApiResponse<PageResult<IndicatorResponse>>> listIndicators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) Integer year) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Indicator> indicatorPage;

        if (year != null) {
            // 按年份过滤：通过 cycle -> task -> indicator 关系链
            indicatorPage = strategyApplicationService.getIndicatorsByYear(year, pageable);
        } else if (status != null) {
            indicatorPage = strategyApplicationService.getIndicatorsByStatus(status, pageable);
        } else {
            indicatorPage = strategyApplicationService.getIndicators(pageable);
        }

        Map<Long, String> taskNameMap = buildTaskNameMap(indicatorPage.getContent());
        Map<Long, String> taskTypeMap = buildTaskTypeMap(indicatorPage.getContent());
        Map<Long, List<MilestoneResponse>> milestoneMap = buildMilestoneMap(indicatorPage.getContent());
        Map<Long, CurrentMonthIndicatorRoundState> currentMonthRoundStateMap =
                buildCurrentMonthIndicatorRoundStateMap(indicatorPage.getContent());
        PageResult<IndicatorResponse> result = PageResult.of(
                indicatorPage.getContent().stream()
                        .map(indicator -> toIndicatorResponse(
                                indicator,
                                taskNameMap,
                                taskTypeMap,
                                milestoneMap,
                                currentMonthRoundStateMap
                        ))
                        .toList(),
                (int) indicatorPage.getTotalElements(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> getIndicatorById(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(indicator)));
    }

    @PostMapping
    @Operation(summary = "创建新指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> createIndicator(
            @Valid @RequestBody CreateIndicatorRequest request) {
        // 兼容两种请求格式：
        // 1) 旧格式：indicatorName + departmentId
        // 2) 新格式：indicatorDesc + taskId + ownerOrgId + targetOrgId + parentIndicatorId
        String description = firstNonBlank(
                request.getIndicatorDesc(),
                request.getDescription(),
                request.getIndicatorName()
        );
        String indicatorType = requireIndicatorType(
                request.getType(),
                request.getType1(),
                request.getIndicatorType()
        );
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Indicator description is required");
        }

        Long ownerOrgId = request.getOwnerOrgId() != null ? request.getOwnerOrgId() : request.getDepartmentId();
        if (ownerOrgId == null) {
            throw new IllegalArgumentException("Owner organization is required");
        }
        Long targetOrgId = request.getTargetOrgId() != null ? request.getTargetOrgId() : ownerOrgId;

        SysOrg ownerOrg = organizationRepository.findById(ownerOrgId)
                .orElseThrow(() -> new IllegalArgumentException("Owner organization not found: " + ownerOrgId));
        SysOrg targetOrg = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new IllegalArgumentException("Target organization not found: " + targetOrgId));

        Indicator created = strategyApplicationService.createIndicator(
                description,
                ownerOrg,
                targetOrg,
                request.getTaskId(),
                request.getParentIndicatorId(),
                indicatorType,
                request.getWeightPercent(),
                request.getSortOrder(),
                request.getRemark(),
                request.getProgress()
        );
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(created)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> updateIndicator(
            @PathVariable Long id,
            @RequestBody UpdateIndicatorRequest request) {
        SysOrg ownerOrg = request.getOwnerOrgId() != null
                ? organizationRepository.findById(request.getOwnerOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("Owner organization not found: " + request.getOwnerOrgId()))
                : null;
        SysOrg targetOrg = request.getTargetOrgId() != null
                ? organizationRepository.findById(request.getTargetOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("Target organization not found: " + request.getTargetOrgId()))
                : null;
        String indicatorDesc = request.getIndicatorDesc();
        if ((indicatorDesc == null || indicatorDesc.isBlank()) && request.getIndicatorName() != null) {
            indicatorDesc = request.getIndicatorName();
        }
        Indicator updated = strategyApplicationService.updateIndicator(
                id,
                indicatorDesc,
                request.getWeightPercent(),
                request.getProgress(),
                request.getSortOrder(),
                request.getRemark(),
                request.getTaskId(),
                ownerOrg,
                targetOrg
        );
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除指标")
    public ResponseEntity<ApiResponse<Void>> deleteIndicator(@PathVariable Long id) {
        strategyApplicationService.deleteIndicator(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交指标审核")
    public ResponseEntity<ApiResponse<IndicatorResponse>> submitForReview(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        Indicator submitted = strategyApplicationService.submitIndicatorForReview(indicator);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(submitted)));
    }

    @PostMapping("/{id}/submit-review")
    @Operation(summary = "提交指标审核(旧版别名)")
    public ResponseEntity<ApiResponse<IndicatorResponse>> submitForReviewAlias(@PathVariable Long id) {
        return submitForReview(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> approveIndicator(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        Indicator approved = strategyApplicationService.approveIndicator(indicator);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(approved)));
    }

    @PostMapping("/{id}/approve-review")
    @Operation(summary = "审核通过指标(旧版别名)")
    public ResponseEntity<ApiResponse<IndicatorResponse>> approveIndicatorAlias(@PathVariable Long id) {
        return approveIndicator(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> rejectIndicator(
            @PathVariable Long id,
            @RequestBody RejectRequest request) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }
        // The current service doesn't accept a reason, we'll use a simple version
        Indicator rejected = strategyApplicationService.rejectIndicator(indicator);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(rejected)));
    }

    @PostMapping("/{id}/reject-review")
    @Operation(summary = "拒绝指标(旧版别名)")
    public ResponseEntity<ApiResponse<IndicatorResponse>> rejectIndicatorAlias(
            @PathVariable Long id,
            @RequestBody(required = false) RejectRequest request) {
        return rejectIndicator(id, request != null ? request : new RejectRequest());
    }

    @PostMapping("/{id}/distribute")
    @Operation(summary = "分发指标到目标组织")
    public ResponseEntity<ApiResponse<IndicatorResponse>> distributeIndicator(
            @PathVariable Long id,
            @RequestParam(required = false) Long targetOrgId,
            @RequestParam(required = false) String customDesc,
            @RequestBody(required = false) DistributeIndicatorRequest request) {
        Long resolvedTargetOrgId = targetOrgId;
        String resolvedCustomDesc = customDesc;

        if (request != null) {
            if (resolvedTargetOrgId == null) {
                resolvedTargetOrgId = request.getTargetOrgId();
            }
            if (resolvedTargetOrgId == null && request.getTargetOrgIds() != null && !request.getTargetOrgIds().isEmpty()) {
                resolvedTargetOrgId = request.getTargetOrgIds().get(0);
            }
            if (resolvedCustomDesc == null || resolvedCustomDesc.isBlank()) {
                resolvedCustomDesc = request.getCustomDesc();
            }
            if ((resolvedCustomDesc == null || resolvedCustomDesc.isBlank()) && request.getMessage() != null) {
                resolvedCustomDesc = request.getMessage();
            }
        }

        final Long finalTargetOrgId = resolvedTargetOrgId;
        SysOrg targetOrg = finalTargetOrgId != null
                ? organizationRepository.findById(finalTargetOrgId)
                    .orElseThrow(() -> new IllegalArgumentException("Target organization not found: " + finalTargetOrgId))
                : null;

        Indicator distributed = strategyApplicationService.distributeIndicator(id, targetOrg, resolvedCustomDesc);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(distributed)));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "撤回已分发的指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> withdrawIndicator(
            @PathVariable Long id,
            @RequestBody(required = false) WithdrawRequest request) {
        Indicator withdrawn = strategyApplicationService.withdrawIndicator(
                id,
                request != null ? request.getReason() : null
        );
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(withdrawn)));
    }

    @PostMapping("/batch-withdraw")
    @Operation(summary = "批量撤回指标(按所属和目标组织)")
    public ResponseEntity<ApiResponse<BatchWithdrawResponse>> batchWithdrawIndicators(
            @RequestBody @Valid BatchWithdrawRequest request) {
        BatchWithdrawResponse result = strategyApplicationService.batchWithdrawIndicators(
                request.getOwnerOrgId(),
                request.getTargetOrgId(),
                request.getReason()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/actions/batch-distribute")
    @Operation(summary = "批量分发指标(分发页面专用)")
    public ResponseEntity<ApiResponse<BatchDistributeIndicatorsResponse>> batchDistributeIndicators(
            @RequestBody @Valid BatchDistributeIndicatorsRequest request) {
        List<BatchDistributeIndicatorsResponse.ItemResult> items = request.getIndicators().stream()
                .map(item -> {
                    Indicator distributed;

                    if (item.getIndicatorId() != null) {
                        SysOrg targetOrg = organizationRepository.findById(item.getTargetOrgId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Target organization not found: " + item.getTargetOrgId()));
                        distributed = strategyApplicationService.distributeIndicator(
                                item.getIndicatorId(),
                                targetOrg,
                                item.getCustomDesc()
                        );
                    } else {
                        if (item.getOwnerOrgId() == null) {
                            throw new IllegalArgumentException("Owner organization is required");
                        }
                        if (item.getTaskId() == null) {
                            throw new IllegalArgumentException("Task ID is required");
                        }

                        String indicatorDesc = firstNonBlank(item.getIndicatorDesc(), item.getCustomDesc());
                        String indicatorType = requireIndicatorType(
                                item.getType(),
                                item.getType1(),
                                item.getIndicatorType()
                        );
                        if (indicatorDesc == null || indicatorDesc.isBlank()) {
                            throw new IllegalArgumentException("Indicator description is required");
                        }

                        SysOrg ownerOrg = organizationRepository.findById(item.getOwnerOrgId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Owner organization not found: " + item.getOwnerOrgId()));
                        SysOrg targetOrg = organizationRepository.findById(item.getTargetOrgId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Target organization not found: " + item.getTargetOrgId()));

                        Indicator created = strategyApplicationService.createIndicator(
                                indicatorDesc,
                                ownerOrg,
                                targetOrg,
                                item.getTaskId(),
                                item.getParentIndicatorId(),
                                indicatorType,
                                item.getWeightPercent(),
                                item.getSortOrder(),
                                item.getRemark(),
                                item.getProgress()
                        );
                        distributed = strategyApplicationService.distributeIndicator(
                                created.getId(),
                                targetOrg,
                                item.getCustomDesc()
                        );
                    }

                    return new BatchDistributeIndicatorsResponse.ItemResult(
                            item.getClientRequestId(),
                            distributed.getId()
                    );
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                new BatchDistributeIndicatorsResponse(items.size(), items)
        ));
    }

    @GetMapping("/search")
    @Operation(summary = "按关键词搜索指标")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> searchIndicators(
            @RequestParam String keyword) {
        List<Indicator> result = strategyApplicationService.searchIndicators(keyword);
        List<IndicatorResponse> responses = result.stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "根据任务ID获取指标")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> getIndicatorsByTaskId(@PathVariable Long taskId) {
        List<Indicator> indicators = strategyApplicationService.getIndicatorsByTaskId(taskId);
        List<IndicatorResponse> responses = indicators.stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/task/{taskId}/root")
    @Operation(summary = "根据任务ID获取根指标")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> getRootIndicatorsByTaskId(@PathVariable Long taskId) {
        List<IndicatorResponse> responses = strategyApplicationService.getRootIndicatorsByTaskId(taskId).stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/owner/{orgId}")
    @Operation(summary = "根据所属组织ID获取指标")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> getIndicatorsByOwnerOrg(@PathVariable Long orgId) {
        List<IndicatorResponse> responses = strategyApplicationService.getIndicatorsByOwnerOrgId(orgId).stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/target/{orgId}")
    @Operation(summary = "根据目标组织ID获取指标")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> getIndicatorsByTargetOrg(@PathVariable Long orgId) {
        List<IndicatorResponse> responses = strategyApplicationService.getIndicatorsByTargetOrgId(orgId).stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/distribution-eligibility")
    @Operation(summary = "检查指标分发 eligibility")
    public ResponseEntity<ApiResponse<DistributionEligibilityResponse>> getDistributionEligibility(@PathVariable Long id) {
        Indicator indicator = strategyApplicationService.getIndicatorById(id);
        if (indicator == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "Indicator not found"));
        }

        boolean eligible = Objects.equals(indicator.getStatus(), IndicatorStatus.DRAFT);
        String reason = eligible ? null : "Only draft indicators can be distributed";
        List<DistributionTargetOrgResponse> availableTargetOrgs = organizationRepository.findAll().stream()
                .filter(org -> Boolean.TRUE.equals(org.getIsActive()))
                .filter(org -> indicator.getOwnerOrg() == null || !Objects.equals(org.getId(), indicator.getOwnerOrg().getId()))
                .map(org -> new DistributionTargetOrgResponse(org.getId(), org.getName()))
                .toList();

        DistributionEligibilityResponse response = new DistributionEligibilityResponse(
                eligible,
                reason,
                availableTargetOrgs
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/distributed")
    @Operation(summary = "获取已分发的子指标")
    public ResponseEntity<ApiResponse<List<IndicatorResponse>>> getDistributedIndicators(@PathVariable Long id) {
        List<IndicatorResponse> responses = strategyApplicationService.getDistributedIndicators(id).stream()
                .map(this::toIndicatorResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/breakdown")
    @Operation(summary = "分解指标为子指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> breakdownIndicator(@PathVariable Long id) {
        Indicator brokenDown = strategyApplicationService.breakdownIndicator(id);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(brokenDown)));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> activateIndicator(@PathVariable Long id) {
        Indicator activated = strategyApplicationService.activateIndicator(id);
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(activated)));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止指标")
    public ResponseEntity<ApiResponse<IndicatorResponse>> terminateIndicator(
            @PathVariable Long id,
            @RequestBody TerminateRequest request) {
        Indicator terminated = strategyApplicationService.terminateIndicator(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(toIndicatorResponse(terminated)));
    }

    // ==================== Helper Methods ====================

    private IndicatorResponse toIndicatorResponse(Indicator indicator) {
        return toIndicatorResponse(
                indicator,
                Map.of(),
                Map.of(),
                buildMilestoneMap(List.of(indicator)),
                buildCurrentMonthIndicatorRoundStateMap(List.of(indicator))
        );
    }

    private IndicatorResponse toIndicatorResponse(Indicator indicator, Map<Long, String> taskNameMap) {
        return toIndicatorResponse(
                indicator,
                taskNameMap,
                buildTaskTypeMap(List.of(indicator)),
                buildMilestoneMap(List.of(indicator)),
                buildCurrentMonthIndicatorRoundStateMap(List.of(indicator))
        );
    }

    private IndicatorResponse toIndicatorResponse(Indicator indicator,
                                                 Map<Long, String> taskNameMap,
                                                 Map<Long, String> taskTypeMap,
                                                 Map<Long, List<MilestoneResponse>> milestoneMap,
                                                 Map<Long, CurrentMonthIndicatorRoundState> currentMonthRoundStateMap) {
        IndicatorResponse response = new IndicatorResponse();
        response.setId(indicator.getId());
        response.setTaskId(indicator.getTaskId());
        if (indicator.getTaskId() != null) {
            String taskName = taskNameMap.get(indicator.getTaskId());
            if (taskName == null) {
                com.sism.task.domain.StrategicTask task = taskRepository.findById(indicator.getTaskId()).orElse(null);
                if (task != null) {
                    taskName = task.getName();
                    if (task.getTaskType() != null) {
                        response.setTaskType(task.getTaskType().name());
                    }
                } else {
                    taskName = "计划-" + indicator.getTaskId();
                }
            }
            response.setTaskName(taskName);
            if (response.getTaskType() == null) {
                response.setTaskType(taskTypeMap.get(indicator.getTaskId()));
            }
        }
        response.setIndicatorDesc(indicator.getIndicatorDesc());
        response.setIndicatorName(indicator.getIndicatorDesc()); // Using desc as name for now
        response.setParentIndicatorId(indicator.getParentIndicatorId());
        response.setWeightPercent(indicator.getWeightPercent());
        response.setSortOrder(indicator.getSortOrder());
        response.setStatus(indicator.getStatus() != null ? indicator.getStatus().toString() : null);
        response.setLevel(indicator.getLevel() != null ? indicator.getLevel().toString() : null);
        response.setProgress(indicator.getProgress());
        CurrentMonthIndicatorRoundState roundState = currentMonthRoundStateMap.get(indicator.getId());
        response.setReportProgress(roundState != null ? roundState.progress() : null);
        response.setHasCurrentMonthFill(roundState != null && roundState.hasCurrentMonthFill());
        response.setRemark(indicator.getRemark());
        response.setIndicatorType(indicator.getType());
        response.setCreatedAt(indicator.getCreatedAt());
        response.setUpdatedAt(indicator.getUpdatedAt());
        if (indicator.getOwnerOrg() != null) {
            response.setOwnerOrgId(indicator.getOwnerOrg().getId());
            response.setOwnerOrgName(indicator.getOwnerOrg().getName());
        }
        if (indicator.getTargetOrg() != null) {
            response.setTargetOrgId(indicator.getTargetOrg().getId());
            response.setTargetOrgName(indicator.getTargetOrg().getName());
        }
        response.setMilestones(milestoneMap.getOrDefault(indicator.getId(), List.of()));
        return response;
    }

    private Map<Long, String> buildTaskNameMap(List<Indicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return Map.of();
        }

        List<Long> taskIds = indicators.stream()
                .map(Indicator::getTaskId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        if (taskIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> taskNameMap = new HashMap<>();
        jpaTaskRepository.findTaskNamesByIds(taskIds)
                .forEach(task -> taskNameMap.put(task.getId(), task.getName()));
        return taskNameMap;
    }

    private Map<Long, List<MilestoneResponse>> buildMilestoneMap(List<Indicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return Map.of();
        }

        List<Long> indicatorIds = indicators.stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        if (indicatorIds.isEmpty()) {
            return Map.of();
        }

        return milestoneApplicationService.getAllMilestones().stream()
                .filter(milestone -> milestone.getIndicatorId() != null && indicatorIds.contains(milestone.getIndicatorId()))
                .collect(java.util.stream.Collectors.groupingBy(
                        MilestoneResponse::getIndicatorId,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }

    private Map<Long, String> buildTaskTypeMap(List<Indicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return Map.of();
        }

        List<Long> taskIds = indicators.stream()
                .map(Indicator::getTaskId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        if (taskIds.isEmpty()) {
            return Map.of();
        }

        return taskRepository.findAllById(taskIds).stream()
                .filter(task -> task.getId() != null)
                .collect(Collectors.toMap(
                        com.sism.task.domain.StrategicTask::getId,
                        task -> task.getTaskType() != null ? task.getTaskType().name() : null,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, CurrentMonthIndicatorRoundState> buildCurrentMonthIndicatorRoundStateMap(List<Indicator> indicators) {
        List<Long> indicatorIds = extractIndicatorIds(indicators);
        if (indicatorIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = indicatorIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String currentMonth = currentReportMonth();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT latest.indicator_id AS indicator_id,
                       latest.report_progress AS report_progress,
                       latest.report_status AS report_status
                FROM (
                    SELECT pri.indicator_id AS indicator_id,
                           pri.progress AS report_progress,
                           pr.status AS report_status,
                           ROW_NUMBER() OVER (
                               PARTITION BY pri.indicator_id
                               ORDER BY pr.created_at DESC, pr.id DESC
                           ) AS row_no
                    FROM public.plan_report_indicator pri
                    INNER JOIN public.plan_report pr ON pri.report_id = pr.id
                    WHERE pr.is_deleted = false
                      AND pr.report_month = ?
                      AND pri.indicator_id IN (%s)
                ) latest
                WHERE latest.row_no = 1
                """.formatted(placeholders),
                Stream.concat(Stream.of(currentMonth), indicatorIds.stream()).toArray()
        );

        Map<Long, CurrentMonthIndicatorRoundState> roundStateByIndicatorId = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object indicatorIdValue = row.get("indicator_id");
            Object reportProgressValue = row.get("report_progress");
            Object reportStatusValue = row.get("report_status");
            if (!(indicatorIdValue instanceof Number indicatorIdNumber)) {
                continue;
            }
            String normalizedStatus = reportStatusValue == null
                    ? ""
                    : String.valueOf(reportStatusValue).trim().toUpperCase();
            boolean hasEditableCurrentRound = !normalizedStatus.isEmpty()
                    && !"APPROVED".equals(normalizedStatus)
                    && !"REJECTED".equals(normalizedStatus);
            Integer reportProgress = reportProgressValue instanceof Number reportProgressNumber
                    ? reportProgressNumber.intValue()
                    : null;
            roundStateByIndicatorId.put(
                    indicatorIdNumber.longValue(),
                    new CurrentMonthIndicatorRoundState(
                            hasEditableCurrentRound ? reportProgress : null,
                            hasEditableCurrentRound
                    )
            );
        }
        return roundStateByIndicatorId;
    }

    private List<Long> extractIndicatorIds(List<Indicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return List.of();
        }

        return indicators.stream()
                .map(Indicator::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    /**
     * 获取指标的最新填报进度
     * 从 plan_report_indicator 表中查询最新的填报进度
     *
     * @param indicatorId 指标ID
     * @return 最新填报进度，如果没有则返回 null
     */
    private Integer getLatestReportProgress(Long indicatorId) {
        if (indicatorId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT pri.progress
                    FROM public.plan_report_indicator pri
                    INNER JOIN public.plan_report pr ON pri.report_id = pr.id
                    WHERE pri.indicator_id = ?
                    AND pr.is_deleted = false
                    ORDER BY pr.created_at DESC
                    LIMIT 1
                    """,
                    Integer.class,
                    indicatorId
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查指标当前月份是否有填报数据
     * 判断依据：plan_report 表中当前月份是否有该指标关联的填报记录
     *
     * @param indicatorId 指标ID
     * @return true-有填报数据显示"编辑"，false-无数据显示"填报"
     */
    private Boolean hasCurrentMonthFill(Long indicatorId) {
        if (indicatorId == null) {
            return false;
        }
        try {
            String currentMonth = currentReportMonth();
            String latestStatus = jdbcTemplate.queryForObject(
                    """
                    SELECT pr.status
                    FROM public.plan_report_indicator pri
                    INNER JOIN public.plan_report pr ON pri.report_id = pr.id
                    WHERE pri.indicator_id = ?
                      AND pr.report_month = ?
                      AND pr.is_deleted = false
                    ORDER BY pr.created_at DESC, pr.id DESC
                    LIMIT 1
                    """,
                    String.class,
                    indicatorId,
                    currentMonth
            );
            String normalizedStatus = latestStatus == null ? "" : latestStatus.trim().toUpperCase();
            return !normalizedStatus.isEmpty()
                    && !"APPROVED".equals(normalizedStatus)
                    && !"REJECTED".equals(normalizedStatus);
        } catch (Exception e) {
            return false;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String optionalIndicatorType(String... values) {
        String value = firstNonBlank(values);
        if (value == null) {
            return null;
        }
        if (!"定量".equals(value) && !"定性".equals(value)) {
            throw new IllegalArgumentException("Indicator type must be either 定量 or 定性");
        }
        return value;
    }

    private String requireIndicatorType(String... values) {
        String value = optionalIndicatorType(values);
        if (value == null) {
            throw new IllegalArgumentException("Indicator type is required");
        }
        return value;
    }

    // ==================== Request/Response DTOs ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorResponse {
        private Long id;
        private Long taskId;
        private String taskName;
        private String taskType;
        private Long parentIndicatorId;
        private String indicatorName;
        private String indicatorCode;
        private String indicatorDesc;
        private Long cycleId;
        private Long ownerOrgId;
        private String ownerOrgName;
        private Long targetOrgId;
        private String targetOrgName;
        private String departmentName;
        private BigDecimal targetValue;
        private String unit;
        private String status;
        private String dimension;
        private BigDecimal weightPercent;
        private Integer sortOrder;
        private String level;
        private Integer progress;
        private Integer reportProgress;
        private Boolean hasCurrentMonthFill;
        private String remark;
        private String indicatorType;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private List<MilestoneResponse> milestones;
    }

    @Data
    public static class CreateIndicatorRequest {
        private String indicatorName;

        private String indicatorCode;

        private String description;
        private String indicatorDesc;
        private String type;
        private String indicatorType;
        private String type1;
        private Long taskId;
        private Long parentIndicatorId;
        private Long ownerOrgId;
        private Long targetOrgId;
        private BigDecimal weightPercent;
        private Integer sortOrder;
        private String remark;
        private Integer progress;

        private Long cycleId;

        private Long departmentId;

        @DecimalMin(value = "0", message = "Target value must be positive")
        @DecimalMax(value = "100", message = "Target value cannot exceed 100")
        private BigDecimal targetValue;

        private String unit;
        private String dimension; // FINANCIAL, OPERATION, etc.
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }

    @Data
    public static class UpdateIndicatorRequest {
        private String indicatorName;
        private String indicatorDesc;
        private Long taskId;
        private Long ownerOrgId;
        private Long targetOrgId;
        private BigDecimal weightPercent;
        private Integer progress;
        private Integer sortOrder;
        private String remark;
    }

    @Data
    public static class DistributeIndicatorRequest {
        private Long targetOrgId;
        private List<Long> targetOrgIds;
        private String customDesc;
        private String message;
        private String deadline;
        private Long actorUserId;
    }

    @Data
    public static class WithdrawRequest {
        private String reason;
    }

    @Data
    public static class TerminateRequest {
        private String reason;
    }

    @Data
    public static class BatchWithdrawRequest {
        private Long ownerOrgId;
        private Long targetOrgId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchWithdrawResponse {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private List<Long> withdrawnIndicatorIds;
        private List<String> errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionEligibilityResponse {
        private boolean eligible;
        private String reason;
        private List<DistributionTargetOrgResponse> availableTargetOrgs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionTargetOrgResponse {
        private Long orgId;
        private String orgName;
    }
}
