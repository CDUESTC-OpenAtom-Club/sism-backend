package com.sism.workflow.application;

import com.sism.strategy.domain.event.PlanSubmittedForApprovalEvent;
import com.sism.workflow.interfaces.dto.WorkflowInstanceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanWorkflowEventListenerTest {

    @Mock
    private BusinessWorkflowApplicationService businessWorkflowApplicationService;

    @Test
    void shouldRetryTransientPlanWorkflowStartFailures() {
        PlanWorkflowEventListener listener = new PlanWorkflowEventListener(businessWorkflowApplicationService);

        when(businessWorkflowApplicationService.startWorkflow(any(), eq(188L), eq(35L)))
                .thenThrow(new RuntimeException("transient-1"))
                .thenThrow(new RuntimeException("transient-2"))
                .thenReturn(WorkflowInstanceResponse.builder().instanceId("501").build());

        listener.handlePlanSubmittedForApproval(new PlanSubmittedForApprovalEvent(7075L, "PLAN_DISPATCH_STRATEGY", 188L, 35L));

        verify(businessWorkflowApplicationService, times(3)).startWorkflow(any(), eq(188L), eq(35L));
    }

    @Test
    void shouldNotRetryIllegalStatePlanWorkflowStartFailures() {
        PlanWorkflowEventListener listener = new PlanWorkflowEventListener(businessWorkflowApplicationService);

        when(businessWorkflowApplicationService.startWorkflow(any(), eq(188L), eq(35L)))
                .thenThrow(new IllegalStateException("active workflow exists"));

        assertThrows(
                IllegalStateException.class,
                () -> listener.handlePlanSubmittedForApproval(
                        new PlanSubmittedForApprovalEvent(7075L, "PLAN_DISPATCH_STRATEGY", 188L, 35L))
        );

        verify(businessWorkflowApplicationService, times(1)).startWorkflow(any(), eq(188L), eq(35L));
    }
}
