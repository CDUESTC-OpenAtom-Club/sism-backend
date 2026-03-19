package com.sism.workflow.application.query;

import com.sism.workflow.application.definition.WorkflowDefinitionQueryService;
import com.sism.workflow.domain.definition.model.AuditFlowDef;
import com.sism.workflow.domain.query.repository.WorkflowQueryRepository;
import com.sism.workflow.domain.runtime.model.AuditInstance;
import com.sism.workflow.domain.runtime.model.AuditStepInstance;
import com.sism.workflow.domain.runtime.repository.AuditInstanceRepository;
import com.sism.workflow.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowReadModelService {

    private final WorkflowDefinitionQueryService workflowDefinitionQueryService;
    private final AuditInstanceRepository auditInstanceRepository;
    private final WorkflowQueryRepository workflowQueryRepository;
    private final WorkflowReadModelMapper workflowReadModelMapper;

    public PageResult<WorkflowDefinitionResponse> listDefinitions(int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<AuditFlowDef> page = workflowDefinitionQueryService.getAllAuditFlowDefs(pageable);
        List<WorkflowDefinitionResponse> items = page.getContent().stream()
                .map(workflowReadModelMapper::toDefinitionResponse)
                .toList();
        return PageResult.of(items, page.getTotalElements(), pageNum, pageSize);
    }

    public PageResult<WorkflowInstanceResponse> listInstances(String definitionId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<AuditInstance> page = auditInstanceRepository.findByFlowDefId(Long.parseLong(definitionId), pageable);
        List<WorkflowInstanceResponse> items = page.getContent().stream()
                .map(workflowReadModelMapper::toInstanceResponse)
                .toList();
        return PageResult.of(items, page.getTotalElements(), pageNum, pageSize);
    }

    public WorkflowInstanceDetailResponse getInstanceDetail(String instanceId) {
        AuditInstance instance = auditInstanceRepository.findById(Long.parseLong(instanceId))
                .orElseThrow(() -> new IllegalArgumentException("Workflow instance not found: " + instanceId));

        WorkflowInstanceDetailResponse response = new WorkflowInstanceDetailResponse();
        WorkflowInstanceResponse base = workflowReadModelMapper.toInstanceResponse(instance);
        response.setInstanceId(base.getInstanceId());
        response.setDefinitionId(base.getDefinitionId());
        response.setStatus(base.getStatus());
        response.setBusinessEntityId(base.getBusinessEntityId());
        response.setStarterId(base.getStarterId());
        response.setStartTime(base.getStartTime());
        response.setEndTime(base.getEndTime());

        List<WorkflowTaskResponse> tasks = instance.getStepInstances().stream()
                .sorted(Comparator.comparing(step -> step.getStepIndex() == null ? Integer.MAX_VALUE : step.getStepIndex()))
                .map(workflowReadModelMapper::toTaskResponse)
                .toList();
        response.setTasks(tasks);
        response.setHistory(buildHistory(instance));
        return response;
    }

    public PageResult<WorkflowTaskResponse> getMyPendingTasks(Long userId, int pageNum) {
        int pageSize = 10;
        List<AuditInstance> pendingInstances = workflowQueryRepository.findPendingAuditInstancesByUserId(userId);

        List<WorkflowTaskResponse> tasks = pendingInstances.stream()
                .flatMap(instance -> instance.getStepInstances().stream()
                        .filter(step -> AuditInstance.STEP_STATUS_PENDING.equals(step.getStatus()))
                        .filter(step -> userId.equals(step.getApproverId()))
                        .map(step -> workflowReadModelMapper.toPendingTaskResponse(instance, step)))
                .sorted(Comparator.comparing(WorkflowTaskResponse::getCreatedTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, tasks.size());
        List<WorkflowTaskResponse> pagedTasks = start < tasks.size() ? tasks.subList(start, end) : List.of();
        return PageResult.of(pagedTasks, tasks.size(), pageNum, pageSize);
    }

    private List<WorkflowHistoryResponse> buildHistory(AuditInstance instance) {
        List<WorkflowHistoryResponse> history = new ArrayList<>();

        if (instance.getStartedAt() != null) {
            history.add(WorkflowHistoryResponse.builder()
                    .historyId(instance.getId() + "_start")
                    .taskId(instance.getId().toString())
                    .taskName(workflowReadModelMapper.buildInstanceLabel(instance))
                    .operatorId(instance.getRequesterId())
                    .operatorName("Initiator")
                    .action("START")
                    .comment("Workflow started")
                    .operateTime(instance.getStartedAt())
                    .build());
        }

        for (AuditStepInstance step : instance.getStepInstances()) {
            if (AuditInstance.STEP_STATUS_APPROVED.equals(step.getStatus())) {
                history.add(workflowReadModelMapper.toHistoryResponse(instance, step, "APPROVE"));
            } else if (AuditInstance.STEP_STATUS_REJECTED.equals(step.getStatus())) {
                history.add(workflowReadModelMapper.toHistoryResponse(instance, step, "REJECT"));
            }
        }

        if (instance.getCompletedAt() != null) {
            String action = AuditInstance.STATUS_APPROVED.equals(instance.getStatus()) ? "FINISH_APPROVE"
                    : AuditInstance.STATUS_REJECTED.equals(instance.getStatus()) ? "FINISH_REJECT"
                    : "CANCEL";
            history.add(WorkflowHistoryResponse.builder()
                    .historyId(instance.getId() + "_finish")
                    .taskId(instance.getId().toString())
                    .taskName(workflowReadModelMapper.buildInstanceLabel(instance))
                    .operatorId(instance.getRequesterId())
                    .operatorName("System")
                    .action(action)
                    .comment(action)
                    .operateTime(instance.getCompletedAt())
                    .build());
        }

        return history;
    }
}
