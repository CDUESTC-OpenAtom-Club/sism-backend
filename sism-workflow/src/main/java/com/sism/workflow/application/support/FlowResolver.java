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

    public void resolveAndAttachFlow(com.sism.workflow.domain.runtime.model.AuditInstance instance) {
        if (instance.getFlowDefId() != null) {
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
        List<String> preferredCodes = "TASK".equalsIgnoreCase(entityType)
                ? List.of("PLAN_DISPATCH_STRATEGY", "PLAN_DISPATCH_FUNCDEPT", "INDICATOR_DEFAULT_APPROVAL")
                : List.of("INDICATOR_DEFAULT_APPROVAL", "PLAN_DISPATCH_STRATEGY", "PLAN_DISPATCH_FUNCDEPT");

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

    public String normalizeEntityTypeForFlow(String entityType) {
        if (entityType == null) {
            return "INDICATOR";
        }
        if ("TASK".equalsIgnoreCase(entityType)) {
            return "INDICATOR";
        }
        return entityType;
    }
}
