package com.sism.workflow.interfaces.assembler;

import com.sism.workflow.domain.definition.AuditFlowDef;
import com.sism.workflow.domain.definition.AuditStepDef;
import com.sism.workflow.domain.runtime.AuditInstance;
import com.sism.workflow.domain.runtime.WorkflowTask;
import com.sism.workflow.interfaces.dto.CreateLegacyFlowRequest;
import com.sism.workflow.interfaces.dto.CreateLegacyFlowStepRequest;
import com.sism.workflow.interfaces.dto.StartLegacyInstanceRequest;
import com.sism.workflow.interfaces.dto.WorkflowTaskStartRequest;
import org.springframework.stereotype.Component;

/**
 * LegacyWorkflowAssembler - 旧审批接口的 DTO 到领域对象转换器。
 * 统一收敛控制器中的手工字段拷贝，避免遗漏敏感字段或引入脆弱的赋值逻辑。
 */
@Component
public class LegacyWorkflowAssembler {

    public AuditFlowDef toAuditFlowDef(CreateLegacyFlowRequest request) {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowCode(request.getFlowCode());
        flowDef.setFlowName(request.getFlowName());
        flowDef.setDescription(request.getDescription());
        flowDef.setEntityType(request.getEntityType());
        flowDef.setIsActive(request.getIsActive());
        flowDef.setVersion(request.getVersion());

        if (request.getSteps() != null) {
            request.getSteps().forEach(stepRequest -> flowDef.addStep(toAuditStepDef(stepRequest)));
        }

        return flowDef;
    }

    public AuditStepDef toAuditStepDef(CreateLegacyFlowStepRequest request) {
        AuditStepDef step = new AuditStepDef();
        step.setStepOrder(request.getStepOrder());
        step.setStepName(request.getStepName());
        step.setStepType(request.getStepType());
        step.setRoleId(request.getRoleId());
        step.setIsRequired(request.getIsRequired());
        step.setIsTerminal(request.getIsTerminal());
        return step;
    }

    public AuditInstance toAuditInstance(StartLegacyInstanceRequest request) {
        AuditInstance instance = new AuditInstance();
        instance.setFlowDefId(request.getFlowDefId());
        instance.setEntityType(request.getEntityType());
        instance.setEntityId(request.getEntityId());
        return instance;
    }

    public WorkflowTask toWorkflowTask(WorkflowTaskStartRequest request) {
        WorkflowTask task = new WorkflowTask();
        task.setWorkflowId(request.getWorkflowId());
        task.setWorkflowType(request.getWorkflowType());
        task.setTaskName(request.getTaskName());
        task.setTaskType(request.getTaskType());
        task.setCurrentStep(request.getCurrentStep());
        task.setNextStep(request.getNextStep());
        task.setInitiatorId(request.getInitiatorId());
        task.setInitiatorOrgId(request.getInitiatorOrgId());
        task.setDueDate(request.getDueDate());
        return task;
    }
}
