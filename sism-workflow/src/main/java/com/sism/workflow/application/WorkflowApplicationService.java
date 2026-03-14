package com.sism.workflow.application;

import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.shared.domain.model.workflow.AuditFlowDef;
import com.sism.shared.domain.model.workflow.AuditInstance;
import com.sism.shared.domain.model.workflow.WorkflowTask;
import com.sism.workflow.domain.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return workflowRepository.findAllAuditInstances();
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
        instance.start(requesterId, requesterOrgId);
        instance = workflowRepository.saveAuditInstance(instance);
        publishAndSaveEvents(instance);
        return instance;
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
