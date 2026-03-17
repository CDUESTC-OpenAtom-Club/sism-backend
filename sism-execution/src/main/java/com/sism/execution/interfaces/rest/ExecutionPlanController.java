package com.sism.execution.interfaces.rest;

import com.sism.execution.application.ExecutionApplicationService;
import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/execution/plans")
@RequiredArgsConstructor
@Tag(name = "Execution Plans", description = "Execution plan management endpoints")
public class ExecutionPlanController {

    private final ExecutionApplicationService executionApplicationService;

    @PostMapping
    @Operation(summary = "Create a new plan")
    public ResponseEntity<Plan> createPlan(
            @RequestParam Long cycleId,
            @RequestParam Long targetOrgId,
            @RequestParam Long createdByOrgId,
            @RequestParam PlanLevel planLevel) {
        Plan created = executionApplicationService.createPlan(cycleId, targetOrgId, createdByOrgId, planLevel);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a plan")
    public ResponseEntity<Plan> activatePlan(@PathVariable Long id) {
        Plan plan = executionApplicationService.getPlanById(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        Plan activated = executionApplicationService.activatePlan(plan);
        return ResponseEntity.ok(activated);
    }

    @GetMapping
    @Operation(summary = "Get all plans")
    public ResponseEntity<java.util.List<Plan>> getAllPlans() {
        return ResponseEntity.ok(executionApplicationService.getAllPlans());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID")
    public ResponseEntity<Plan> getPlanById(@PathVariable Long id) {
        Plan plan = executionApplicationService.getPlanById(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }
}
