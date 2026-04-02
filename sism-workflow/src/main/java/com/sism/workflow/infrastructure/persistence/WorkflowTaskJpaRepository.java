package com.sism.workflow.infrastructure.persistence;

import com.sism.workflow.domain.runtime.model.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * WorkflowTaskJpaRepository - 工作流任务 JPA Repository
 */
@Repository
public interface WorkflowTaskJpaRepository extends JpaRepository<WorkflowTask, Long> {

    @Query("SELECT t FROM WorkflowRuntimeTask t WHERE t.status = :status")
    List<WorkflowTask> findByStatus(@Param("status") String status);

    @Query("SELECT t FROM WorkflowRuntimeTask t WHERE t.assigneeId = :assigneeId")
    List<WorkflowTask> findByAssigneeId(@Param("assigneeId") Long assigneeId);

    @Query("SELECT t FROM WorkflowRuntimeTask t WHERE t.workflowId = :workflowId")
    List<WorkflowTask> findByWorkflowId(@Param("workflowId") String workflowId);
}
