package com.sism.workflow.application;

import com.sism.execution.domain.model.report.event.PlanReportSubmittedEvent;
import com.sism.execution.domain.model.report.event.PlanReportApprovedEvent;
import com.sism.execution.domain.model.report.event.PlanReportRejectedEvent;
import com.sism.workflow.interfaces.dto.StartWorkflowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    private final BusinessWorkflowApplicationService businessWorkflowService;

    /**
     * 处理"计划报告已提交"事件
     * 当用户在 sism-execution 模块中提交报告时，自动启动报告审批工作流
     *
     * @param event PlanReportSubmittedEvent 事件，包含报告ID、月份和组织ID
     */
    @EventListener
    @Transactional
    public void handlePlanReportSubmitted(PlanReportSubmittedEvent event) {
        if (event == null) {
            log.warn("Received null PlanReportSubmittedEvent");
            return;
        }

        try {
            log.info("=== [工作流事件监听器] 接收到报告提交事件 ===");
            log.info("报告ID: {}, 报告月份: {}, 报告组织ID: {}",
                    event.getReportId(), event.getReportMonth(), event.getReportOrgId());

            // 构建工作流启动请求
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setWorkflowCode("REPORT_APPROVAL");  // 报告审批工作流代码
            request.setBusinessEntityId(event.getReportId());
            request.setBusinessEntityType("PlanReport");
            request.setVariables(buildWorkflowVariables(event));

            // 启动工作流实例
            // userId 和 orgId 应该从事件中获取，或者从上下文中获取
            // 这里使用 1L 作为系统管理员用户，实际应该从事件中获取
            var response = businessWorkflowService.startWorkflow(
                    request,
                    1L,  // systemAdminUserId - TODO: 从事件或审计信息中获取
                    event.getReportOrgId()
            );

            log.info("✅ 工作流启动成功 - 工作流实例ID: {}, 报告ID: {}",
                    response.getInstanceId(), event.getReportId());

        } catch (IllegalStateException e) {
            // 可能是因为已经存在活跃的工作流实例
            log.warn("⚠️ 无法启动工作流（可能是因为已存在活跃实例）: reportId={}, reason={}",
                    event.getReportId(), e.getMessage());
            // 不抛出异常，继续处理其他事件
        } catch (Exception e) {
            log.error("❌ 启动工作流失败: reportId={}, 错误信息={}",
                    event.getReportId(), e.getMessage(), e);
            // 记录错误但不阻塞事件处理流程
            // 在生产环境中，可能需要存储这个失败事件以供后续重试
        }
    }

    /**
     * 处理"计划报告已批准"事件
     * 可选：如果需要在工作流完成后执行额外的业务逻辑
     *
     * @param event PlanReportApprovedEvent 事件
     */
    @EventListener
    @Transactional
    public void handlePlanReportApproved(PlanReportApprovedEvent event) {
        if (event == null) {
            log.warn("Received null PlanReportApprovedEvent");
            return;
        }

        try {
            log.info("=== [工作流事件监听器] 接收到报告批准事件 ===");
            log.info("报告ID: {}, 报告月份: {}, 报告组织ID: {}",
                    event.getReportId(), event.getReportMonth(), event.getReportOrgId());

            // TODO: 在这里实现报告批准后的业务逻辑
            // 例如：触发数据汇总、发送通知等
            log.info("✅ 报告批准事件已记录，可在此处添加后续业务逻辑");

        } catch (Exception e) {
            log.error("❌ 处理报告批准事件失败: reportId={}, 错误信息={}",
                    event.getReportId(), e.getMessage(), e);
        }
    }

    /**
     * 处理"计划报告已驳回"事件
     * 可选：如果需要在工作流驳回后执行额外的业务逻辑
     *
     * @param event PlanReportRejectedEvent 事件
     */
    @EventListener
    @Transactional
    public void handlePlanReportRejected(PlanReportRejectedEvent event) {
        if (event == null) {
            log.warn("Received null PlanReportRejectedEvent");
            return;
        }

        try {
            log.info("=== [工作流事件监听器] 接收到报告驳回事件 ===");
            log.info("报告ID: {}, 报告月份: {}, 报告组织ID: {}, 驳回原因: {}",
                    event.getReportId(), event.getReportMonth(), event.getReportOrgId(),
                    event.getReason());

            // TODO: 在这里实现报告驳回后的业务逻辑
            // 例如：发送回复通知、重置报告状态等
            log.info("✅ 报告驳回事件已记录，可在此处添加后续业务逻辑");

        } catch (Exception e) {
            log.error("❌ 处理报告驳回事件失败: reportId={}, 错误信息={}",
                    event.getReportId(), e.getMessage(), e);
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
        variables.put("eventId", event.getEventId());
        variables.put("occurredOn", event.getOccurredOn());
        return variables;
    }
}
