package com.sism.workflow.application.support;

import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.definition.repository.FlowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FlowResolver {

    private final FlowDefinitionRepository flowDefinitionRepository;

    /**
     * 自动解析并挂载审批流程定义到实例。
     * PlanReport 类型不参与自动解析——职能部门月报和学院月报使用不同流程，
     * 必须由调用方（ReportWorkflowEventListener）通过 workflowCode 显式指定。
     */
    public void resolveAndAttachFlow(com.sism.workflow.domain.runtime.model.AuditInstance instance) {
        if (instance.getFlowDefId() != null) {
            return;
        }

        if (isPlanReportEntityType(instance.getEntityType())) {
            // PlanReport 存在 FUNC / COLLEGE 两条流程，resolver 无法区分，
            // 必须由业务入口显式设置 flowDefId，此处不做猜测。
            return;
        }

        Optional<AuditFlowDef> resolvedByCode = findFlowByPreferredCode(instance.getEntityType());
        if (resolvedByCode.isPresent()) {
            instance.setFlowDefId(resolvedByCode.get().getId());
            return;
        }

        String lookupEntityType = normalizeEntityTypeForFlow(instance.getEntityType());
        List<AuditFlowDef> defs = flowDefinitionRepository.findByEntityType(lookupEntityType);
        defs.stream()
                .filter(flow -> Boolean.TRUE.equals(flow.getIsActive()))
                .findFirst()
                .ifPresent(flow -> instance.setFlowDefId(flow.getId()));
    }

    public Optional<AuditFlowDef> findFlowByPreferredCode(String entityType) {
        String normalized = normalizeEntityTypeForFlow(entityType);
        List<String> preferredCodes = resolvePreferredCodes(entityType);

        for (String code : preferredCodes) {
            Optional<AuditFlowDef> found = flowDefinitionRepository.findByCode(code)
                    .filter(flow -> Boolean.TRUE.equals(flow.getIsActive()))
                    .filter(flow -> normalized.equalsIgnoreCase(flow.getEntityType()));
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private List<String> resolvePreferredCodes(String entityType) {
        if ("TASK".equalsIgnoreCase(entityType)) {
            return List.of("PLAN_DISPATCH_STRATEGY", "PLAN_DISPATCH_FUNCDEPT", "INDICATOR_DEFAULT_APPROVAL");
        }
        return List.of("INDICATOR_DEFAULT_APPROVAL", "PLAN_DISPATCH_STRATEGY", "PLAN_DISPATCH_FUNCDEPT");
    }

    public String normalizeEntityTypeForFlow(String entityType) {
        if (entityType == null) {
            return "INDICATOR";
        }
        if ("TASK".equalsIgnoreCase(entityType)) {
            return "INDICATOR";
        }
        if (isPlanReportEntityType(entityType)) {
            return "PlanReport";
        }
        return entityType;
    }

    private boolean isPlanReportEntityType(String entityType) {
        return "PlanReport".equalsIgnoreCase(entityType) || "PLAN_REPORT".equalsIgnoreCase(entityType);
    }
}
