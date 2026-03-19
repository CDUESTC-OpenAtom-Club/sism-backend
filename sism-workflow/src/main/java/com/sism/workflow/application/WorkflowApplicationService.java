package com.sism.workflow.application;

import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.shared.domain.model.workflow.AuditFlowDef;
import com.sism.shared.domain.model.workflow.AuditInstance;
import com.sism.shared.domain.model.workflow.AuditStepDef;
import com.sism.shared.domain.model.workflow.AuditStepInstance;
import com.sism.shared.domain.model.workflow.WorkflowTask;
import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.workflow.domain.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

/**
 * WorkflowApplicationService - 工作流应用服务
 * 负责协调工作流相关的业务操作，包括事件的持久化和发布
 */
@Service
@RequiredArgsConstructor
public class WorkflowApplicationService {

    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;
    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;

    // ==================== Audit Flow Definition Methods ====================

    public List<AuditFlowDef> getAllAuditFlowDefs() {
        return workflowRepository.findAllAuditFlowDefs();
    }

    public AuditFlowDef getAuditFlowDefById(Long id) {
        return workflowRepository.findAuditFlowDefById(id).orElse(null);
    }

    public AuditFlowDef getAuditFlowDefByCode(String flowCode) {
        return workflowRepository.findAuditFlowDefByCode(flowCode).orElse(null);
    }

    public List<AuditFlowDef> getAuditFlowDefsByEntityType(String entityType) {
        return workflowRepository.findAuditFlowDefsByEntityType(entityType);
    }

    /**
     * 创建审批流定义
     */
    @Transactional
    public AuditFlowDef createAuditFlowDef(AuditFlowDef flowDef) {
        flowDef.validate();
        flowDef = workflowRepository.saveAuditFlowDef(flowDef);
        publishAndSaveEvents(flowDef);
        return flowDef;
    }

    // ==================== Audit Instance Methods ====================

    public List<AuditInstance> getAllAuditInstances() {
        return workflowRepository.findAll();
    }

    public AuditInstance getAuditInstanceById(Long instanceId) {
        return workflowRepository.findAuditInstanceById(instanceId).orElse(null);
    }

    public List<AuditInstance> getPendingAuditInstancesByUserId(Long userId) {
        return workflowRepository.findPendingAuditInstancesByUserId(userId);
    }

    public List<AuditInstance> getApprovedAuditInstancesByUserId(Long userId) {
        return workflowRepository.findApprovedAuditInstancesByUserId(userId);
    }

    public List<AuditInstance> getAppliedAuditInstancesByUserId(Long userId) {
        return workflowRepository.findAppliedAuditInstancesByUserId(userId);
    }

    public List<AuditInstance> getAuditInstanceHistory(Long instanceId) {
        return workflowRepository.findAuditInstanceHistory(instanceId);
    }

    /**
     * 启动审批实例
     */
    @Transactional
    public AuditInstance startAuditInstance(AuditInstance instance, Long requesterId, Long requesterOrgId) {
        instance.validate();
        resolveAndAttachFlow(instance);
        instance.start(requesterId, requesterOrgId);
        initializeStepInstances(instance, requesterId, requesterOrgId);
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
    }

    private void resolveAndAttachFlow(AuditInstance instance) {
        if (instance.getFlowDefId() != null) {
            return;
        }

        Optional<AuditFlowDef> resolvedByCode = findFlowByPreferredCode(instance.getEntityType());
        if (resolvedByCode.isPresent()) {
            instance.setFlowDefId(resolvedByCode.get().getId());
            return;
        }

        String lookupEntityType = normalizeEntityTypeForFlow(instance.getEntityType());
        List<AuditFlowDef> defs = workflowRepository.findAuditFlowDefsByEntityType(lookupEntityType);
        defs.stream()
                .filter(flow -> Boolean.TRUE.equals(flow.getIsActive()))
                .findFirst()
                .ifPresent(flow -> instance.setFlowDefId(flow.getId()));
    }

    private Optional<AuditFlowDef> findFlowByPreferredCode(String entityType) {
        String normalized = normalizeEntityTypeForFlow(entityType);
        List<String> preferredCodes = "TASK".equalsIgnoreCase(entityType)
                ? List.of("PLAN_DISPATCH_STRATEGY", "PLAN_DISPATCH_FUNCDEPT", "INDICATOR_DEFAULT_APPROVAL")
                : List.of("INDICATOR_DEFAULT_APPROVAL", "PLAN_DISPATCH_STRATEGY", "PLAN_DISPATCH_FUNCDEPT");

        for (String code : preferredCodes) {
            Optional<AuditFlowDef> found = workflowRepository.findAuditFlowDefByCode(code)
                    .filter(flow -> Boolean.TRUE.equals(flow.getIsActive()))
                    .filter(flow -> normalized.equalsIgnoreCase(flow.getEntityType()));
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private String normalizeEntityTypeForFlow(String entityType) {
        if (entityType == null) {
            return "INDICATOR";
        }
        if ("TASK".equalsIgnoreCase(entityType)) {
            return "INDICATOR";
        }
        return entityType;
    }

    private void initializeStepInstances(AuditInstance instance, Long requesterId, Long requesterOrgId) {
        if (instance.getStepInstances() != null && !instance.getStepInstances().isEmpty()) {
            return;
        }

        List<AuditStepDef> stepDefs = List.of();
        if (instance.getFlowDefId() != null) {
            AuditFlowDef flowDef = workflowRepository.findAuditFlowDefById(instance.getFlowDefId()).orElse(null);
            if (flowDef != null && flowDef.getSteps() != null) {
                stepDefs = flowDef.getSteps();
            }
        }

        if (stepDefs.isEmpty()) {
            AuditStepInstance fallback = new AuditStepInstance();
            fallback.setStepIndex(1);
            fallback.setStepName("默认审批");
            fallback.setStatus(AuditInstance.STEP_STATUS_PENDING);
            fallback.setApproverId(requesterId);
            fallback.setApproverName("User-" + requesterId);
            instance.addStepInstance(fallback);
            instance.setCurrentStepIndex(1);
            return;
        }

        // Step definitions in DB may have null/duplicate step_no in historical data.
        // Use deterministic order: step_no first, then id, to keep approval routing stable.
        List<AuditStepDef> orderedStepDefs = stepDefs.stream()
                .sorted(Comparator
                        .comparing((AuditStepDef step) -> step.getStepOrder() == null ? Integer.MAX_VALUE : step.getStepOrder())
                        .thenComparing(step -> step.getId() == null ? Long.MAX_VALUE : step.getId()))
                .toList();

        int order = 1;
        for (AuditStepDef stepDef : orderedStepDefs) {
            AuditStepInstance step = new AuditStepInstance();
            Integer stepOrder = stepDef.getStepOrder();
            int index = stepOrder != null && stepOrder > 0 ? stepOrder : order;
            step.setStepIndex(index);
            step.setStepName(stepDef.getStepName() != null ? stepDef.getStepName() : "审批步骤" + index);
            Long resolvedApproverId = resolveApproverId(stepDef, requesterId, requesterOrgId);
            step.setApproverId(resolvedApproverId);
            step.setApproverName(resolveApproverName(resolvedApproverId));
            step.setStatus(order == 1 ? AuditInstance.STEP_STATUS_PENDING : AuditInstance.STEP_STATUS_WAITING);
            instance.addStepInstance(step);
            order++;
        }
        autoCompleteSubmissionStep(instance, requesterId);
    }

    private void autoCompleteSubmissionStep(AuditInstance instance, Long requesterId) {
        if (instance.getStepInstances() == null || instance.getStepInstances().isEmpty()) {
            return;
        }

        AuditStepInstance firstStep = instance.getStepInstances().stream()
                .sorted(Comparator.comparing(step -> step.getStepIndex() == null ? Integer.MAX_VALUE : step.getStepIndex()))
                .findFirst()
                .orElse(null);

        if (firstStep == null) {
            return;
        }

        instance.setCurrentStepIndex(firstStep.getStepIndex());

        if (!shouldAutoCompleteSubmissionStep(firstStep, requesterId)) {
            return;
        }

        firstStep.setStatus(AuditInstance.STEP_STATUS_APPROVED);
        firstStep.setApprovedAt(LocalDateTime.now());
        firstStep.setComment("系统自动完成提交流程节点");

        AuditStepInstance nextStep = instance.getStepInstances().stream()
                .filter(step -> !step.equals(firstStep))
                .filter(step -> AuditInstance.STEP_STATUS_WAITING.equals(step.getStatus()))
                .sorted(Comparator.comparing(step -> step.getStepIndex() == null ? Integer.MAX_VALUE : step.getStepIndex()))
                .findFirst()
                .orElse(null);

        if (nextStep != null) {
            nextStep.setStatus(AuditInstance.STEP_STATUS_PENDING);
            instance.setCurrentStepIndex(nextStep.getStepIndex());
        }
    }

    private boolean shouldAutoCompleteSubmissionStep(AuditStepInstance firstStep, Long requesterId) {
        if (!AuditInstance.STEP_STATUS_PENDING.equals(firstStep.getStatus())) {
            return false;
        }

        String stepName = firstStep.getStepName() != null ? firstStep.getStepName().trim() : "";
        boolean isSubmissionStep = stepName.contains("提交");
        if (!isSubmissionStep) {
            return false;
        }

        return requesterId != null && requesterId.equals(firstStep.getApproverId());
    }

    private Long resolveApproverId(AuditStepDef stepDef, Long requesterId, Long requesterOrgId) {
        String approverType = stepDef.getApproverType() != null ? stepDef.getApproverType().trim().toUpperCase() : "";
        Long approverRef = stepDef.getApproverId();

        if ("USER".equals(approverType) && approverRef != null && approverRef > 0) {
            return approverRef;
        }

        if ("ROLE".equals(approverType) && approverRef != null && approverRef > 0) {
            List<User> candidates = userRepository.findByRoleId(approverRef).stream()
                    .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                    .toList();

            Optional<User> sameOrgCandidate = candidates.stream()
                    .filter(user -> requesterOrgId != null && requesterOrgId.equals(user.getOrgId()))
                    .min(Comparator.comparing(User::getId));
            if (sameOrgCandidate.isPresent()) {
                return sameOrgCandidate.get().getId();
            }

            return candidates.stream()
                    .min(Comparator.comparing(User::getId))
                    .map(User::getId)
                    .orElse(requesterId);
        }

        // 兼容未配置 approver_type 的老数据：回退到发起人，避免流程阻塞
        return requesterId;
    }

    private String resolveApproverName(Long userId) {
        if (userId == null || userId <= 0) {
            return "Unknown";
        }
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getRealName() != null && !user.getRealName().isBlank()) {
                        return user.getRealName();
                    }
                    return user.getUsername() != null ? user.getUsername() : "User-" + userId;
                })
                .orElse("User-" + userId);
    }

    /**
     * 审批通过
     */
    @Transactional
    public AuditInstance approveAuditInstance(AuditInstance instance, Long userId, String comment) {
        instance.approve(userId, comment);
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public AuditInstance rejectAuditInstance(AuditInstance instance, Long userId, String comment) {
        instance.reject(userId, comment);
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
    }

    /**
     * 取消审批实例
     */
    @Transactional
    public AuditInstance cancelAuditInstance(AuditInstance instance) {
        instance.cancel();
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
    }

    /**
     * 转交审批实例
     */
    @Transactional
    public AuditInstance transferAuditInstance(Long instanceId, Long targetUserId) {
        AuditInstance instance = workflowRepository.findAuditInstanceById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Audit instance not found: " + instanceId));
        instance.transfer(targetUserId);
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
    }

    /**
     * 添加审批人
     */
    @Transactional
    public AuditInstance addApproverToInstance(Long instanceId, Long approverId) {
        AuditInstance instance = workflowRepository.findAuditInstanceById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Audit instance not found: " + instanceId));
        instance.addApprover(approverId);
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
    }

    // ==================== Workflow Task Methods ====================

    /**
     * 启动工作流任务
     */
    @Transactional
    public WorkflowTask startWorkflowTask(WorkflowTask task, Long operatorId, Long operatorOrgId) {
        task.validate();
        task.start(operatorId, operatorOrgId);
        task = workflowRepository.saveWorkflowTask(task);
        publishAndSaveEvents(task);
        return task;
    }

    /**
     * 完成工作流任务
     */
    @Transactional
    public WorkflowTask completeWorkflowTask(WorkflowTask task, String result) {
        task.complete(result);
        task = workflowRepository.saveWorkflowTask(task);
        publishAndSaveEvents(task);
        return task;
    }

    /**
     * 工作流任务失败
     */
    @Transactional
    public WorkflowTask failWorkflowTask(WorkflowTask task, String errorMessage) {
        task.fail(errorMessage);
        task = workflowRepository.saveWorkflowTask(task);
        publishAndSaveEvents(task);
        return task;
    }

    /**
     * 审批工作流任务
     */
    @Transactional
    public WorkflowTask approveWorkflowTask(WorkflowTask task, Long approverId, String comment) {
        task.approve(approverId, comment);
        task = workflowRepository.saveWorkflowTask(task);
        publishAndSaveEvents(task);
        return task;
    }

    /**
     * 拒绝工作流任务
     */
    @Transactional
    public WorkflowTask rejectWorkflowTask(WorkflowTask task, Long approverId, String comment) {
        task.reject(approverId, comment);
        task = workflowRepository.saveWorkflowTask(task);
        publishAndSaveEvents(task);
        return task;
    }

    // ==================== Statistics ====================

    public Map<String, Object> getApprovalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstances", workflowRepository.countAuditInstances());
        stats.put("pendingCount", workflowRepository.countPendingAuditInstances());
        stats.put("approvedCount", workflowRepository.countApprovedAuditInstances());
        stats.put("rejectedCount", workflowRepository.countRejectedAuditInstances());
        return stats;
    }

    /**
     * 发布并保存领域事件
     */
    private void publishAndSaveEvents(com.sism.shared.domain.model.base.AggregateRoot<?> aggregate) {
        List<DomainEvent> events = aggregate.getDomainEvents();

        // 保存事件到事件存储
        for (DomainEvent event : events) {
            eventStore.save(event);
        }

        // 发布事件
        eventPublisher.publishAll(events);

        // 清除已发布的事件
        aggregate.clearEvents();
    }
}
