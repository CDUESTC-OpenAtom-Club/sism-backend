package com.sism.workflow.application;

import com.sism.shared.domain.model.workflow.*;
import com.sism.workflow.domain.repository.WorkflowRepository;
import com.sism.workflow.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BusinessWorkflowApplicationService - 业务工作流应用服务
 * 负责业务工作流的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessWorkflowApplicationService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowApplicationService workflowApplicationService;

    // ==================== 工作流启动 ====================

    /**
     * 启动工作流
     */
    @Transactional
    public WorkflowInstanceResponse startWorkflow(StartWorkflowRequest request, Long userId, Long orgId) {
        log.info("Starting workflow: {}, entityId: {}, userId: {}",
                request.getWorkflowCode(), request.getBusinessEntityId(), userId);

        // 1. 检查工作流定义是否存在且已激活
        AuditFlowDef flowDef = workflowRepository.findAuditFlowDefByCode(request.getWorkflowCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow definition not found or not active: " + request.getWorkflowCode()));

        if (!flowDef.getIsActive()) {
            throw new IllegalStateException("Workflow definition is not active: " + request.getWorkflowCode());
        }

        // 2. 检查是否已存在未完成的工作流实例
        String entityType = request.getBusinessEntityType() != null
                ? request.getBusinessEntityType()
                : flowDef.getEntityType();

        boolean hasActive = workflowRepository.hasActiveInstance(
                request.getBusinessEntityId(), entityType);
        if (hasActive) {
            throw new IllegalStateException(
                    "An active workflow already exists for this entity: " + request.getBusinessEntityId());
        }

        // 3. 创建审批实例
        AuditInstance instance = new AuditInstance();
        instance.setFlowDefId(flowDef.getId());
        instance.setEntityType(entityType);
        instance.setEntityId(request.getBusinessEntityId());
        instance.setTitle(flowDef.getFlowName() + " - " + request.getBusinessEntityId());

        // 4. 启动实例
        AuditInstance started = workflowApplicationService.startAuditInstance(
                instance, userId, orgId);

        return convertToInstanceResponse(started);
    }

    /**
     * 通过定义ID启动工作流实例
     */
    @Transactional
    public WorkflowInstanceResponse startWorkflowInstance(
            String definitionId, StartInstanceRequest request, Long userId, Long orgId) {

        log.info("Starting workflow instance by definitionId: {}, entityId: {}, userId: {}",
                definitionId, request.getBusinessEntityId(), userId);

        AuditFlowDef flowDef = workflowRepository.findAuditFlowDefById(Long.parseLong(definitionId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow definition not found: " + definitionId));

        StartWorkflowRequest startRequest = new StartWorkflowRequest();
        startRequest.setWorkflowCode(flowDef.getFlowCode());
        startRequest.setBusinessEntityId(request.getBusinessEntityId());
        startRequest.setBusinessEntityType(flowDef.getEntityType());
        startRequest.setVariables(request.getVariables());

        return startWorkflow(startRequest, userId, orgId);
    }

    // ==================== 工作流查询 ====================

    /**
     * 查询工作流定义列表
     */
    public PageResult<WorkflowDefinitionResponse> listDefinitions(int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<AuditFlowDef> page = workflowRepository.findAllAuditFlowDefs(pageable);

        List<WorkflowDefinitionResponse> items = page.getContent().stream()
                .map(this::convertToDefinitionResponse)
                .collect(Collectors.toList());

        return PageResult.of(items, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 查询工作流实例列表
     */
    public PageResult<WorkflowInstanceResponse> listInstances(
            String definitionId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<AuditInstance> page = workflowRepository.findAuditInstancesByFlowDefId(
                Long.parseLong(definitionId), pageable);

        List<WorkflowInstanceResponse> items = page.getContent().stream()
                .map(this::convertToInstanceResponse)
                .collect(Collectors.toList());

        return PageResult.of(items, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 获取工作流实例详情
     */
    public WorkflowInstanceDetailResponse getInstanceDetail(String instanceId) {
        AuditInstance instance = workflowRepository.findAuditInstanceById(Long.parseLong(instanceId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow instance not found: " + instanceId));

        WorkflowInstanceDetailResponse response = new WorkflowInstanceDetailResponse();
        response.setInstanceId(instance.getId().toString());
        response.setDefinitionId(instance.getFlowDefId() != null ? instance.getFlowDefId().toString() : null);
        response.setStatus(instance.getStatus());
        response.setBusinessEntityId(instance.getEntityId());
        response.setStarterId(instance.getRequesterId());
        response.setStartTime(instance.getStartedAt());
        response.setEndTime(instance.getCompletedAt());

        // 获取任务列表（从 stepInstances 转换）
        List<WorkflowTaskResponse> tasks = instance.getStepInstances().stream()
                .map(step -> new WorkflowTaskResponse(
                        step.getId().toString(),
                        step.getStepName(),
                        "step_" + step.getStepIndex(),
                        convertStepStatus(step.getStatus()),
                        step.getApproverId(),
                        step.getApproverName(),
                        step.getCreatedAt(),
                        null,
                        null,
                        null
                ))
                .collect(Collectors.toList());
        response.setTasks(tasks);

        // 从 stepInstances 构建历史记录
        response.setHistory(buildHistoryFromStepInstances(instance));

        return response;
    }

    /**
     * 从步骤实例构建工作流历史记录
     */
    private List<WorkflowHistoryResponse> buildHistoryFromStepInstances(AuditInstance instance) {
        List<WorkflowHistoryResponse> history = new ArrayList<>();

        // 添加工作流启动记录
        if (instance.getStartedAt() != null) {
            history.add(WorkflowHistoryResponse.builder()
                    .historyId(instance.getId() + "_start")
                    .taskId(instance.getId().toString())
                    .taskName(instance.getTitle())
                    .operatorId(instance.getRequesterId())
                    .operatorName("Initiator")
                    .action("START")
                    .comment("Workflow started")
                    .operateTime(instance.getStartedAt())
                    .build());
        }

        // 添加每个步骤的历史记录
        for (AuditStepInstance step : instance.getStepInstances()) {
            if ("APPROVED".equals(step.getStatus())) {
                history.add(WorkflowHistoryResponse.builder()
                        .historyId(instance.getId() + "_step_" + step.getId())
                        .taskId(instance.getId().toString())
                        .taskName(step.getStepName())
                        .operatorId(step.getApproverId())
                        .operatorName(step.getApproverName())
                        .action("APPROVE")
                        .comment(step.getComment())
                        .operateTime(step.getApprovedAt() != null ? step.getApprovedAt() : step.getCreatedAt())
                        .build());
            } else if ("REJECTED".equals(step.getStatus())) {
                history.add(WorkflowHistoryResponse.builder()
                        .historyId(instance.getId() + "_step_" + step.getId())
                        .taskId(instance.getId().toString())
                        .taskName(step.getStepName())
                        .operatorId(step.getApproverId())
                        .operatorName(step.getApproverName())
                        .action("REJECT")
                        .comment(step.getComment())
                        .operateTime(step.getApprovedAt() != null ? step.getApprovedAt() : step.getCreatedAt())
                        .build());
            }
        }

        // 添加工作流完成记录
        if (instance.getCompletedAt() != null) {
            String action = "APPROVED".equals(instance.getStatus()) ? "FINISH_APPROVE"
                    : "REJECTED".equals(instance.getStatus()) ? "FINISH_REJECT"
                    : "CANCEL";
            history.add(WorkflowHistoryResponse.builder()
                    .historyId(instance.getId() + "_finish")
                    .taskId(instance.getId().toString())
                    .taskName(instance.getTitle())
                    .operatorId(instance.getRequesterId())
                    .operatorName("System")
                    .action(action)
                    .comment(instance.getResult())
                    .operateTime(instance.getCompletedAt())
                    .build());
        }

        return history;
    }

    /**
     * 获取我的待办任务
     */
    public PageResult<WorkflowTaskResponse> getMyPendingTasks(Long userId, int pageNum) {
        int pageSize = 10;

        List<AuditInstance> pendingInstances = workflowRepository.findPendingAuditInstancesByUserId(userId);

        // 简单转换，实际需要从 stepInstances 中提取任务
        List<WorkflowTaskResponse> tasks = pendingInstances.stream()
                .map(instance -> WorkflowTaskResponse.builder()
                        .taskId(instance.getId().toString())
                        .taskName(instance.getTitle())
                        .taskKey("pending_approval")
                        .status("PENDING")
                        .assigneeId(userId)
                        .createdTime(instance.getStartedAt())
                        .build())
                .collect(Collectors.toList());

        // 简单分页
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, tasks.size());
        List<WorkflowTaskResponse> pagedTasks = start < tasks.size()
                ? tasks.subList(start, end)
                : List.of();

        return PageResult.of(pagedTasks, tasks.size(), pageNum, pageSize);
    }

    // ==================== 工作流操作 ====================

    /**
     * 审批任务
     */
    @Transactional
    public WorkflowInstanceResponse approveTask(
            String taskId, ApprovalRequest request, Long userId) {

        log.info("Approving task: {}, userId: {}", taskId, userId);

        // 这里 taskId 实际是 instanceIdId，简化处理
        AuditInstance instance = workflowRepository.findAuditInstanceById(Long.parseLong(taskId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 权限检查：检查当前用户是否是该任务的审批人
        // 检查当前步骤的审批人是否是当前用户
        Optional<AuditStepInstance> currentStep = instance.getStepInstances().stream()
                .filter(step -> "PENDING".equals(step.getStatus()) && userId.equals(step.getApproverId()))
                .findFirst();

        if (currentStep.isEmpty()) {
            // 如果没有找到待审批的步骤，抛出权限异常
            throw new SecurityException("You are not authorized to approve this task");
        }

        AuditInstance approved = workflowApplicationService.approveAuditInstance(
                instance, userId, request.getComment());

        return convertToInstanceResponse(approved);
    }

    /**
     * 拒绝任务
     */
    @Transactional
    public WorkflowInstanceResponse rejectTask(
            String taskId, RejectionRequest request, Long userId) {

        log.info("Rejecting task: {}, userId: {}", taskId, userId);

        AuditInstance instance = workflowRepository.findAuditInstanceById(Long.parseLong(taskId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 权限检查：检查当前用户是否是该任务的审批人
        Optional<AuditStepInstance> currentStep = instance.getStepInstances().stream()
                .filter(step -> "PENDING".equals(step.getStatus()) && userId.equals(step.getApproverId()))
                .findFirst();

        if (currentStep.isEmpty()) {
            throw new SecurityException("You are not authorized to reject this task");
        }

        AuditInstance rejected = workflowApplicationService.rejectAuditInstance(
                instance, userId, request.getReason());

        return convertToInstanceResponse(rejected);
    }

    /**
     * 转办任务
     */
    @Transactional
    public WorkflowInstanceResponse reassignTask(
            String taskId, ReassignRequest request, Long userId) {

        log.info("Reassigning task: {}, fromUserId: {}, toUserId: {}",
                taskId, userId, request.getTargetUserId());

        AuditInstance instance = workflowApplicationService.transferAuditInstance(
                Long.parseLong(taskId), request.getTargetUserId());

        return convertToInstanceResponse(instance);
    }

    /**
     * 取消工作流实例
     */
    @Transactional
    public void cancelInstance(String instanceId, Long userId) {
        log.info("Cancelling instance: {}, userId: {}", instanceId, userId);

        AuditInstance instance = workflowRepository.findAuditInstanceById(Long.parseLong(instanceId))
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // 权限检查：只有发起人可以取消
        if (!instance.getRequesterId().equals(userId)) {
            throw new SecurityException("Only requester can cancel this workflow");
        }

        workflowApplicationService.cancelAuditInstance(instance);
    }

    // ==================== 私有方法 ====================

    private WorkflowDefinitionResponse convertToDefinitionResponse(AuditFlowDef flowDef) {
        return WorkflowDefinitionResponse.builder()
                .definitionId(flowDef.getId().toString())
                .definitionName(flowDef.getFlowName())
                .description(flowDef.getDescription())
                .category(flowDef.getEntityType())
                .version(flowDef.getVersion() != null ? flowDef.getVersion().toString() : "1")
                .isActive(flowDef.getIsActive() != null ? flowDef.getIsActive() : false)
                .createTime(flowDef.getCreatedAt())
                .build();
    }

    private WorkflowInstanceResponse convertToInstanceResponse(AuditInstance instance) {
        return WorkflowInstanceResponse.builder()
                .instanceId(instance.getId().toString())
                .definitionId(instance.getFlowDefId() != null ? instance.getFlowDefId().toString() : null)
                .status(instance.getStatus())
                .businessEntityId(instance.getEntityId())
                .starterId(instance.getRequesterId())
                .startTime(instance.getStartedAt())
                .endTime(instance.getCompletedAt())
                .build();
    }

    private String convertStepStatus(String stepStatus) {
        if (stepStatus == null) {
            return "PENDING";
        }
        return switch (stepStatus.toUpperCase()) {
            case "APPROVED" -> "COMPLETED";
            case "REJECTED" -> "REJECTED";
            default -> "PENDING";
        };
    }
}
