package com.sism.task.application.dto;

import lombok.Data;
import com.sism.task.domain.task.TaskType;

/**
 * TaskQueryRequest - 任务查询请求DTO
 * 用于支持条件查询和分页查询
 */
@Data
public class TaskQueryRequest {

    /**
     * 计划ID（可选）
     */
    private Long planId;

    /**
     * 考核周期ID（可选）
     */
    private Long cycleId;

    /**
     * 组织ID（可选）
     */
    private Long orgId;

    /**
     * 创建组织ID（可选）
     */
    private Long createdByOrgId;

    /**
     * 任务类型（可选）
     */
    private TaskType taskType;

    private String planStatus;

    private String taskStatus;

    /**
     * 任务名称模糊搜索（可选）
     */
    private String name;

    /**
     * 页码（可选，默认0）
     */
    private Integer page = 0;

    /**
     * 每页大小（可选，默认10）
     */
    private Integer size = 10;

    /**
     * 排序字段（可选）
     */
    private String sortBy;

    /**
     * 排序方向（可选，asc/desc）
     */
    private String sortDirection;
}
