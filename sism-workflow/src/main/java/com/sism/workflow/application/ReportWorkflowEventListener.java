package com.sism.workflow.application;

import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.application.ReportApplicationService;
import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.event.PlanReportApprovedEvent;
import com.sism.execution.domain.model.report.event.PlanReportRejectedEvent;
import com.sism.execution.domain.model.report.event.PlanReportSubmittedEvent;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.StartWorkflowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;
import java.util.List;

/**
 * ReportWorkflowEventListener - 报告相关事件的工作流监听器
 * 监听来自 sism-execution 模块的报告事件，并自动启动对应的审批工作流
 *
 * 事件流：
 * 1. 用户在 sism-execution 模块中提交报告
 * 2. PlanReport 聚合根生成 PlanReportSubmittedEvent 事件
 * 3. DomainEventPublisher 发布事件到 Spring 事件系统
 * 4. 此监听器接收事件（异步）
 * 5. 启动 sism-workflow 模块中的报告审批工作流
 *
 * 这是连接 sism-execution 和 sism-workflow 两个模块的关键桥梁。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportWorkflowEventListener {

    private static final String PLAN_REPORT_ENTITY_TYPE = "PLAN_REPORT";
    private static final String REPORT_STATUS_APPROVED = "APPROVED";
    private static final String REPORT_STATUS_REJECTED = "REJECTED";

    private final BusinessWorkflowApplicationService businessWorkflowService;
    private final WorkflowApplicationService workflowApplicationService;
    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowTerminalStatusSyncService workflowTerminalStatusSyncService;
    private final ReportApplicationService reportApplicationService;

    /**
     * 处理"计划报告已提交"事件
     * 当用户在 sism-execution 模块中提交报告时，自动启动报告审批工作流
     *
     * @param event PlanReportSubmittedEvent 事件，包含报告ID、月份和组织ID
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanReportSubmitted(PlanReportSubmittedEvent event) {
        if (event == null) {
            log.warn("Received null PlanReportSubmittedEvent");
            return;
        }

        try {
            log.info("=== [工作流事件监听器] 接收到报告提交事件 ===");
            log.info("报告ID: {}, 报告月份: {}, 报告组织ID: {}",
                    event.getReportId(), event.getReportMonth(), event.getReportOrgId());

            PlanReport report = reportApplicationService.findReportById(event.getReportId())
                    .orElse(null);
            ReportOrgType reportOrgType = report == null ? null : report.getReportOrgType();
            if (reportOrgType == null) {
                throw new IllegalArgumentException("Report not found: " + event.getReportId());
            }

            Long auditInstanceId = report == null ? null : report.getAuditInstanceId();

            AuditInstance resumableInstance = findResumableReportInstance(event.getReportId(), auditInstanceId);
            if (resumableInstance != null) {
                AuditInstance resumed = workflowApplicationService.resumeWithdrawnAuditInstance(resumableInstance);
                reportApplicationService.attachAuditInstance(event.getReportId(), resumed.getId());
                log.info("✅ 已恢复既有报告审批实例 - instanceId: {}, reportId: {}",
                        resumed.getId(), event.getReportId());
                return;
            }

            // 构建工作流启动请求
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setWorkflowCode(resolveWorkflowCode(reportOrgType));
            request.setBusinessEntityId(event.getReportId());
            request.setBusinessEntityType(PLAN_REPORT_ENTITY_TYPE);
            request.setVariables(buildWorkflowVariables(event));

            Long initiatorId = resolveSubmitterId(event);

            var response = businessWorkflowService.startWorkflow(
                    request,
                    initiatorId,
                    event.getReportOrgId()
            );

            log.info("✅ 工作流启动成功 - 工作流实例ID: {}, 报告ID: {}",
                    response.getInstanceId(), event.getReportId());
            if (response.getInstanceId() != null) {
                reportApplicationService.attachAuditInstance(event.getReportId(), Long.parseLong(response.getInstanceId()));
            }

        } catch (IllegalStateException e) {
            // 可能是因为已经存在活跃的工作流实例
            log.warn("⚠️ 无法启动工作流（可能是因为已存在活跃实例）: reportId={}, reason={}",
                    event.getReportId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ 启动工作流失败: reportId={}, 错误信息={}",
                    event.getReportId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to start report workflow for reportId=" + event.getReportId(), e);
        }
    }

    /**
     * 处理"计划报告已批准"事件
     * 可选：如果需要在工作流完成后执行额外的业务逻辑
     *
     * @param event PlanReportApprovedEvent 事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanReportApproved(PlanReportApprovedEvent event) {
        if (event == null) {
            log.warn("Received null PlanReportApprovedEvent");
            return;
        }

        try {
            log.info("=== [工作流事件监听器] 接收到报告批准事件 ===");
            log.info("报告ID: {}, 报告月份: {}, 报告组织ID: {}, 审批人: {}",
                    event.getReportId(), event.getReportMonth(), event.getReportOrgId(), event.getApproverId());

            if (!hasReportStatus(event.getReportId(), REPORT_STATUS_APPROVED)) {
                throw new IllegalStateException("Report is not approved yet: " + event.getReportId());
            }

            int syncedCount = workflowTerminalStatusSyncService.syncReportWorkflowTerminalStatus(
                    event.getReportId(),
                    AuditInstance.STATUS_APPROVED,
                    event.getApproverId(),
                    "Plan report approved"
            );
            log.info("✅ 报告批准后置处理完成，同步工作流实例数量: {}", syncedCount);

        } catch (Exception e) {
            log.error("❌ 处理报告批准事件失败: reportId={}, 错误信息={}",
                    event.getReportId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to process approved report event for reportId=" + event.getReportId(), e);
        }
    }

    /**
     * 处理"计划报告已驳回"事件
     * 可选：如果需要在工作流驳回后执行额外的业务逻辑
     *
     * @param event PlanReportRejectedEvent 事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanReportRejected(PlanReportRejectedEvent event) {
        if (event == null) {
            log.warn("Received null PlanReportRejectedEvent");
            return;
        }

        try {
            log.info("=== [工作流事件监听器] 接收到报告驳回事件 ===");
            log.info("报告ID: {}, 报告月份: {}, 报告组织ID: {}, 驳回人: {}, 驳回原因: {}",
                    event.getReportId(), event.getReportMonth(), event.getReportOrgId(),
                    event.getApproverId(), event.getReason());

            if (!hasReportStatus(event.getReportId(), REPORT_STATUS_REJECTED)) {
                throw new IllegalStateException("Report is not rejected yet: " + event.getReportId());
            }

            int syncedCount = workflowTerminalStatusSyncService.syncReportWorkflowTerminalStatus(
                    event.getReportId(),
                    AuditInstance.STATUS_REJECTED,
                    event.getApproverId(),
                    event.getReason()
            );
            log.info("✅ 报告驳回后置处理完成，同步工作流实例数量: {}", syncedCount);

        } catch (Exception e) {
            log.error("❌ 处理报告驳回事件失败: reportId={}, 错误信息={}",
                    event.getReportId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to process rejected report event for reportId=" + event.getReportId(), e);
        }
    }

    /**
     * 构建工作流变量
     * 将报告事件中的信息转换为工作流所需的变量
     *
     * @param event PlanReportSubmittedEvent 事件
     * @return 工作流变量映射
     */
    private java.util.Map<String, Object> buildWorkflowVariables(PlanReportSubmittedEvent event) {
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("reportId", event.getReportId());
        variables.put("reportMonth", event.getReportMonth());
        variables.put("reportOrgId", event.getReportOrgId());
        variables.put("submitterId", event.getSubmitterId());
        return variables;
    }

    private Long resolveSubmitterId(PlanReportSubmittedEvent event) {
        if (event.getSubmitterId() == null) {
            throw new IllegalArgumentException("PlanReportSubmittedEvent missing submitterId: " + event.getReportId());
        }
        return event.getSubmitterId();
    }

    private boolean hasReportStatus(Long reportId, String expectedStatus) {
        PlanReport report = reportApplicationService.findReportById(reportId).orElse(null);
        return report != null && expectedStatus.equalsIgnoreCase(report.getStatus());
    }

    private String resolveWorkflowCode(ReportOrgType reportOrgType) {
        if (reportOrgType == ReportOrgType.COLLEGE) {
            return "PLAN_APPROVAL_COLLEGE";
        }
        return "PLAN_APPROVAL_FUNCDEPT";
    }

    private AuditInstance findResumableReportInstance(Long reportId, Long boundAuditInstanceId) {
        if (boundAuditInstanceId != null && boundAuditInstanceId > 0) {
            AuditInstance boundInstance = auditInstanceRepository.findById(boundAuditInstanceId).orElse(null);
            if (isResumableReportInstance(boundInstance)) {
                return boundInstance;
            }
        }

        List<AuditInstance> candidates = java.util.stream.Stream.concat(
                        auditInstanceRepository.findByBusinessTypeAndBusinessId(PLAN_REPORT_ENTITY_TYPE, reportId).stream(),
                        auditInstanceRepository.findByBusinessTypeAndBusinessId("PlanReport", reportId).stream())
                .distinct()
                .toList();

        return candidates.stream()
                .filter(this::isResumableReportInstance)
                .max(Comparator
                        .comparing(AuditInstance::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditInstance::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean isResumableReportInstance(AuditInstance instance) {
        if (instance == null) {
            return false;
        }

        String status = String.valueOf(instance.getStatus()).trim().toUpperCase();
        if (!AuditInstance.STATUS_PENDING.equals(status)
                && !AuditInstance.STATUS_REJECTED.equals(status)
                && !AuditInstance.STATUS_WITHDRAWN.equals(status)) {
            return false;
        }

        return instance.getStepInstances().stream()
                .anyMatch(step -> AuditInstance.STEP_STATUS_WITHDRAWN.equals(step.getStatus())
                        && isSubmitterReturnStep(step, instance));
    }

    private boolean isSubmitterReturnStep(AuditStepInstance step, AuditInstance instance) {
        if (step == null) {
            return false;
        }

        if (instance != null && instance.getRequesterId() != null
                && instance.getRequesterId().equals(step.getApproverId())) {
            return true;
        }

        String stepName = step.getStepName();
        return stepName != null && stepName.contains("提交");
    }
}
