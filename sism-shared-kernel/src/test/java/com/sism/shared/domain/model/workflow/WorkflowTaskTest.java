package com.sism.shared.domain.model.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowTaskTest {

    @Test
    void shouldOnlyStartFromPending() {
        WorkflowTask task = new WorkflowTask();
        task.setStatus(WorkflowTask.STATUS_COMPLETED);

        assertThrows(IllegalStateException.class, () -> task.start(1L, 2L));
    }

    @Test
    void shouldOnlyCompleteFromRunning() {
        WorkflowTask task = new WorkflowTask();
        task.setStatus(WorkflowTask.STATUS_PENDING);

        assertThrows(IllegalStateException.class, () -> task.complete("done"));
    }

    @Test
    void shouldOnlyFailFromRunning() {
        WorkflowTask task = new WorkflowTask();
        task.setStatus(WorkflowTask.STATUS_PENDING);

        assertThrows(IllegalStateException.class, () -> task.fail("error"));
    }

    @Test
    void shouldOnlyCancelFromPendingOrRunning() {
        WorkflowTask task = new WorkflowTask();
        task.setStatus(WorkflowTask.STATUS_COMPLETED);

        assertThrows(IllegalStateException.class, task::cancel);
    }

    @Test
    void shouldCancelWhenRunning() {
        WorkflowTask task = new WorkflowTask();
        task.setStatus(WorkflowTask.STATUS_RUNNING);

        task.cancel();

        assertEquals(WorkflowTask.STATUS_CANCELLED, task.getStatus());
    }
}
