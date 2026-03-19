package com.sism.workflow.application;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.application.query.WorkflowReadModelMapper;
import com.sism.workflow.application.query.WorkflowReadModelService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * BusinessWorkflowApplicationService - 业务工作流应用服务
 * 负责业务工作流的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessWorkflowApplicationService {

    private final WorkflowDefinitionQueryService workflowDefinitionQueryService;
    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowQueryRepository workflowQueryRepository;
    private final WorkflowApplicationService workflowApplicationService;
    private final WorkflowReadModelService workflowReadModelService;
    private final WorkflowReadModelMapper workflowReadModelMapper;

    // ==================== 工作流启动 ====================

    /**
     * 启动工作流
     */
    @Transactional
    public WorkflowInstanceResponse startWorkflow(StartWorkflowRequest request, Long userId, Long orgId) {
        log.info("Starting workflow: {}, entityId: {}, userId: {}",
                request.getWorkflowCode(), request.getBusinessEntityId(), userId);

        // 1. 检查工作流定义是否存在且已激活
        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefByCode(request.getWorkflowCode());
        if (flowDef == null) {
            throw new IllegalArgumentException("Workflow definition not found or not active: " + request.getWorkflowCode());
        }

        if (!flowDef.getIsActive()) {
            throw new IllegalStateException("Workflow definition is not active: " + request.getWorkflowCode());
        }

        // 2. 检查是否已存在未完成的工作流实例
        String entityType = request.getBusinessEntityType() != null
                ? request.getBusinessEntityType()
                : flowDef.getEntityType();

        boolean hasActive = auditInstanceRepository.hasActiveInstance(
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

        // 4. 启动实例
        AuditInstance started = workflowApplicationService.startAuditInstance(
                instance, userId, orgId);

        return workflowReadModelMapper.toInstanceResponse(started);
    }

    /**
     * 通过定义ID启动工作流实例
     */
    @Transactional
    public WorkflowInstanceResponse startWorkflowInstance(
            String definitionId, StartInstanceRequest request, Long userId, Long orgId) {

        log.info("Starting workflow instance by definitionId: {}, entityId: {}, userId: {}",
                definitionId, request.getBusinessEntityId(), userId);

        AuditFlowDef flowDef = workflowDefinitionQueryService.getAuditFlowDefById(Long.parseLong(definitionId));
        if (flowDef == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
        }

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
        return workflowReadModelService.listDefinitions(pageNum, pageSize);
    }

    /**
     * 查询工作流实例列表
     */
    public PageResult<WorkflowInstanceResponse> listInstances(
            String definitionId, int pageNum, int pageSize) {
        return workflowReadModelService.listInstances(definitionId, pageNum, pageSize);
    }

    /**
     * 获取工作流实例详情
     */
    public WorkflowInstanceDetailResponse getInstanceDetail(String instanceId) {
        return workflowReadModelService.getInstanceDetail(instanceId);
    }

    /**
     * 获取我的待办任务
     */
    public PageResult<WorkflowTaskResponse> getMyPendingTasks(Long userId, int pageNum) {
        return workflowReadModelService.getMyPendingTasks(userId, pageNum);
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
        AuditInstance instance = auditInstanceRepository.findById(Long.parseLong(taskId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 权限检查：检查当前用户是否是该任务的审批人
        // 检查当前步骤的审批人是否是当前用户
        Optional<AuditStepInstance> currentStep = instance.resolveCurrentPendingStep()
                .filter(step -> userId.equals(step.getApproverId()));

        if (currentStep.isEmpty()) {
            // 如果没有找到待审批的步骤，抛出权限异常
            throw new SecurityException("You are not authorized to approve this task");
        }

        AuditInstance approved = workflowApplicationService.approveAuditInstance(
                instance, userId, request.getComment());

        return workflowReadModelMapper.toInstanceResponse(approved);
    }

    /**
     * 拒绝任务
     */
    @Transactional
    public WorkflowInstanceResponse rejectTask(
            String taskId, RejectionRequest request, Long userId) {

        log.info("Rejecting task: {}, userId: {}", taskId, userId);

        AuditInstance instance = auditInstanceRepository.findById(Long.parseLong(taskId))
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 权限检查：检查当前用户是否是该任务的审批人
        Optional<AuditStepInstance> currentStep = instance.resolveCurrentPendingStep()
                .filter(step -> userId.equals(step.getApproverId()));

        if (currentStep.isEmpty()) {
            throw new SecurityException("You are not authorized to reject this task");
        }

        AuditInstance rejected = workflowApplicationService.rejectAuditInstance(
                instance, userId, request.getReason());

        return workflowReadModelMapper.toInstanceResponse(rejected);
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

        return workflowReadModelMapper.toInstanceResponse(instance);
    }

    /**
     * 取消工作流实例
     */
    @Transactional
    public void cancelInstance(String instanceId, Long userId) {
        log.info("Cancelling instance: {}, userId: {}", instanceId, userId);

        AuditInstance instance = auditInstanceRepository.findById(Long.parseLong(instanceId))
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // 权限检查：只有发起人可以取消
        if (!instance.getRequesterId().equals(userId)) {
            throw new SecurityException("Only requester can cancel this workflow");
        }

        workflowApplicationService.cancelAuditInstance(instance);
    }

    // ==================== 私有方法 ====================
}
