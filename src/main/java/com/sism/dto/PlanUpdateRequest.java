package com.sism.dto;

import com.sism.enums.PlanLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a plan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpdateRequest {

    private Long targetOrgId;

    private PlanLevel planLevel;

    private String status;
}
