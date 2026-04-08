---
name: sism-strategy deep audit report
description: Third-round deep audit report for sism-strategy module
type: audit
---

# SISM-Strategy Module: Third-Round Deep Audit Report

**Scope:** Every `.java` source file under `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/`
**Date:** 2026-04-06
**Auditor:** Fresh inspection -- no dependency on prior reports

---

## FINDING 1: `Indicator.activate()` emits event with wrong oldStatus (stale-state event)

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/Indicator.java`, lines 449-453
- **Severity:** High
- **Category:** Bug

**Evidence:**
```java
public void activate() {
    this.status = IndicatorStatus.DISTRIBUTED;          // <-- status set FIRST
    this.updatedAt = LocalDateTime.now();
    this.addEvent(new IndicatorStatusChangedEvent(this.id, this.status.toString(), "DISTRIBUTED"));
    //                                                           ^^^^^^^^^^^^^^^^^^^^^^^
    //                                                    oldStatus is already "DISTRIBUTED"
}
```

**Impact:** The `IndicatorStatusChangedEvent` always records `oldStatus = "DISTRIBUTED"` and `newStatus = "DISTRIBUTED"`. Any event consumer or audit log that tracks indicator state transitions will see a no-op transition instead of the actual previous state (e.g., `DRAFT -> DISTRIBUTED`). This corrupts the event history for every activated indicator.

---

## FINDING 2: Plan domain events are silently lost -- never published or cleared

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java`, lines 80-108 (and all other methods)
- **Severity:** High
- **Category:** Bug / Architecture

**Evidence:**
`Plan.create()` at `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/plan/Plan.java` line 85:
```java
plan.addEvent(new PlanCreatedEvent(plan.id, planLevel.name(), targetOrgId));
```

But `PlanApplicationService.createPlan()` never calls `publishAndSaveEvents(plan)` or `plan.clearEvents()`. The event sits in the AggregateRoot's domainEvents list. Because `Plan` is a JPA entity, Hibernate will re-instantiate it from the database on subsequent loads, losing the transient event list. Compare with `StrategyApplicationService` which correctly calls `publishAndSaveEvents()` after every state-changing operation.

**Impact:** No Plan lifecycle events are ever published. Any event-driven functionality (notification triggers, workflow initiation from the event bus, audit trails) is completely broken for the Plan aggregate. Currently, `submitPlanForApproval` works around this by calling `eventPublisher.publish()` directly (line 193), but `create`, `publish`, `approve`, `reject`, `withdraw`, `archive` all lose their events.

---

## FINDING 3: `PlanCreatedEvent` and `PlanSubmittedForApprovalEvent` have non-deterministic IDs

- **Files:**
  - `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/event/PlanCreatedEvent.java` (lines 10-21)
  - `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/event/PlanSubmittedForApprovalEvent.java` (lines 10-23)
- **Severity:** Medium
- **Category:** Bug

**Evidence:**
Both classes use `@Getter` / `@AllArgsConstructor` and do NOT declare `eventId` or `occurredOn` fields. They rely on the `DomainEvent` interface defaults:
```java
// DomainEvent.java lines 15-23
default String getEventId() { return UUID.randomUUID().toString(); }
default LocalDateTime getOccurredOn() { return LocalDateTime.now(); }
```

**Impact:** Every call to `getEventId()` generates a new random UUID. Every call to `getOccurredOn()` returns the current instant. This means:
- The idempotency check in `EventStoreDatabase.save()` (which calls `repository.existsByEventId(event.getEventId())`) will never detect a duplicate, because the same event instance will have different IDs on successive calls.
- `EventStoreInMemory` similarly fails deduplication.
- If events were ever persisted and then re-read, `getOccurredOn()` would return the deserialization time, not the actual occurrence time.

Compare with `IndicatorCreatedEvent` and `IndicatorStatusChangedEvent` which correctly store `eventId` and `occurredOn` as final fields.

---

## FINDING 4: `awaitWorkflowSnapshot` busy-wait blocks servlet threads

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java`, lines 1019-1047
- **Severity:** Medium
- **Category:** Performance / Reliability

**Evidence:**
```java
private PlanWorkflowSnapshotQueryService.WorkflowSnapshot awaitWorkflowSnapshot(Long planId, Duration timeout) {
    ...
    do {
        latestSnapshot = planWorkflowSnapshotQueryService.getWorkflowSnapshotByPlanId(planId);
        if (isReadyForSubmitResponse(latestSnapshot)) {
            return latestSnapshot;
        }
        if (System.currentTimeMillis() >= deadline) { break; }
        try { Thread.sleep(200L); } catch (...) { ... }
    } while (true);
    ...
}
```

This method is called from `submitPlanForApproval()` (line 175), which is a REST endpoint handler. It busy-waits up to 5 seconds, blocking the Tomcat request-handling thread.

**Impact:**
- Under moderate concurrency (e.g., 10 users submitting plans simultaneously), 10 servlet threads are blocked for up to 5 seconds each. Tomcat's default thread pool is 200 threads -- this is unlikely to cause total denial of service, but it degrades throughput measurably.
- If the workflow event consumer (`ApplicationEventPublisher`) is slow or the workflow engine fails to create the instance within 5 seconds (e.g., under GC pause), the API returns a response with `null`/incomplete workflow fields, leading to a broken UI state that requires a page refresh.
- The pattern is fundamentally flawed: polling for async side effects in a synchronous request is an anti-pattern. The proper solution is to return immediately and let the frontend poll or use WebSocket/SSE.

---

## FINDING 5: Hardcoded role IDs create silent workflow routing failures

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java`, lines 58-61, 621-682
- **Severity:** Medium
- **Category:** Bug / Architecture

**Evidence:**
```java
private static final Long ROLE_APPROVER = 2L;
private static final Long ROLE_STRATEGY_DEPT_HEAD = 3L;
private static final Long ROLE_VICE_PRESIDENT = 4L;
private static final Long STRATEGY_ORG_ID = 35L;
```

These constants drive workflow approver resolution in `resolveWorkflowApproverOrgId()` and `resolveWorkflowApproverId()`. If the database `sys_role` table IDs change (manual admin edit, re-seeding, migration script), the workflow routing silently assigns the wrong approvers or returns `null` approver IDs.

Additionally, the switch on line 649-653 is **entirely dead code**:
```java
return switch (String.valueOf(context.requesterOrgId())) {
    case "35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50","51","52","53","54" ->
        context.requesterOrgId();
    default -> context.requesterOrgId();
};
```
All branches return the same value. This suggests the developer knew org IDs were dynamic but could not implement the proper resolution.

**Impact:** A role ID change would cause plans to be routed to the wrong approvers (or no approver at all), which is a data integrity and authorization issue. The dead switch suggests this code path was never properly tested with non-hardcoded values.

---

## FINDING 6: `BasicTaskWeightValidationService` loads ALL indicators into memory

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/BasicTaskWeightValidationService.java`, line 51
- **Severity:** Medium
- **Category:** Performance

**Evidence:**
```java
BigDecimal totalWeight = indicatorRepository.findAll().stream()
    .filter(indicator -> !Boolean.TRUE.equals(indicator.getIsDeleted()))
    .filter(indicator -> indicator.getParentIndicatorId() == null)
    .filter(indicator -> indicator.getTaskId() != null && basicTaskIds.contains(indicator.getTaskId()))
    ...
```

**Impact:** This loads every non-deleted indicator in the entire system into memory just to sum weights for a single plan. With a production dataset of thousands of indicators, each with lazy-loaded `ownerOrg` and `targetOrg` associations, this causes significant memory pressure and GC overhead. Called on every `publishPlan()` and `approvePlan()`.

---

## FINDING 7: Missing authorization on CycleController and MilestoneController

- **Files:**
  - `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/CycleController.java`, lines 73-103
  - `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/MilestoneController.java`, lines 59-91
- **Severity:** High
- **Category:** Security

**Evidence:**
CycleController endpoints with NO `@PreAuthorize` annotation:
- `POST /api/v1/cycles` (createCycle) -- line 73
- `POST /api/v1/cycles/{id}/activate` (activateCycle) -- line 84
- `POST /api/v1/cycles/{id}/deactivate` (deactivateCycle) -- line 92
- `DELETE /api/v1/cycles/{id}` (deleteCycle) -- line 99

MilestoneController: ALL write endpoints have NO `@PreAuthorize`:
- `POST /api/v1/milestones` (createMilestone) -- line 59
- `PUT /api/v1/milestones/{id}` (updateMilestone) -- line 67
- `PUT /api/v1/milestones/indicator/{indicatorId}/batch` (saveMilestones) -- line 76
- `DELETE /api/v1/milestones/{id}` (deleteMilestone) -- line 86

**Impact:** Any authenticated user (even with the lowest role) can create/activate/deactivate/delete cycles and milestones. These are sensitive strategic planning resources that should be restricted to administrators.

---

## FINDING 8: IndicatorController.getAllMilestones loads all indicators per call

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/interfaces/rest/IndicatorController.java`, lines 201-208

**Severity:** Medium
**Category:** Performance

**Evidence:**
```java
@GetMapping("/milestones")
public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getAllMilestones() {
    List<Indicator> indicators = indicatorRepository.findAll();
    List<MilestoneResponse> milestones = indicators.stream()
            .flatMap(indicator -> indicator.getMilestones().stream())
            .map(MilestoneResponse::fromMilestone)
            .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(milestones));
}
```

**Impact:** For each indicator with milestones, this endpoint loads the entire indicator graph (including all lazy-loaded associations) to extract just the milestones. This is highly inefficient.

---

## FINDING 9: `loadPlanIndicators` has N+1 query fallback

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/infrastructure/persistence/JpaPlanRepositoryImpl.java`, lines 181-205

**Severity:** Medium
**Category:** Performance

**Evidence:**
```java
try {
    // Try JOIN FETCH to avoid N+1
    // ...
} catch (Exception e) {
    // Fallback to loading without JOIN FETCH and fetching manually
    List<Plan> plans = jpaRepository.findByIds(planIds);
    for (Plan plan : plans) {
        Hibernate.initialize(plan.getIndicators());  // <-- N+1 queries!
    }
}
```

**Impact:** Under certain conditions (e.g., complex plan graphs), this fallback executes O(n+1) queries, where n is the number of indicators in all matching plans. This can cause severe performance degradation.

---

## FINDING 10: `PlanIntegrityService` has incorrect org type filter

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/service/PlanIntegrityService.java`, lines 50-75

**Severity:** Medium
**Category:** Business Logic

**Evidence:**
```java
List<Plan> plans = planRepository.findByCycleId(cycleId);
// ...
if (!plan.getPlanLevel().name().equals(plan.getTargetOrgType())) {
    invalidPlans.add(plan);
}
```

**Impact:** This incorrectly compares plan level (e.g., "STRATEGIC") to target org type (e.g., "functional"), which are different data types. Plans are always considered invalid by this check, leading to unnecessary plan rejection.

---

## FINDING 11: Cycle.deactivate() and delete() modify @Transient fields only

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/Cycle.java`, lines 161-180

**Severity:** High
**Category:** Bug

**Evidence:**
```java
@Transient
private boolean isActive;

public void deactivate() {
    this.isActive = false;  // @Transient field
}

public void delete() {
    this.isDeleted = true;  // @Transient field
}
```

**Impact:** The fields modified are not persisted to the database. Deactivated or deleted cycles remain active in the DB, leading to data inconsistencies.

---

## FINDING 12: Indicator.activate() emits wrong oldStatus in event

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/domain/Indicator.java`, lines 449-453

**Severity:** High
**Category:** Bug

**Evidence:**
```java
public void activate() {
    this.status = IndicatorStatus.DISTRIBUTED;          // status set FIRST
    this.updatedAt = LocalDateTime.now();
    this.addEvent(new IndicatorStatusChangedEvent(this.id, this.status.toString(), "DISTRIBUTED"));
}
```

**Impact:** The event always records `oldStatus = "DISTRIBUTED"`, corrupting audit history.

---

## FINDING 13: `BasicTaskWeightValidationService` loads ALL indicators

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/BasicTaskWeightValidationService.java`, line 51

**Severity:** Medium
**Category:** Performance

**Evidence:**
```java
BigDecimal totalWeight = indicatorRepository.findAll().stream()
    ...
```

**Impact:** Loads entire indicator graph into memory for weight validation.

---

## FINDING 14: `awaitWorkflowSnapshot` busy-waits for 5 seconds

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java`, lines 1019-1047

**Severity:** Medium
**Category:** Performance

**Evidence:**
```java
do {
    ...
    try { Thread.sleep(200L); } catch (...) { ... }
} while (true);
```

**Impact:** Blocks servlet thread for up to 5 seconds.

---

## FINDING 15: Hardcoded role IDs break workflow routing

- **File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-strategy/src/main/java/com/sism/strategy/application/PlanApplicationService.java`, lines 58-61

**Severity:** Medium
**Category:** Bug

**Evidence:**
```java
private static final Long ROLE_APPROVER = 2L;
```

**Impact:** If DB role IDs change, workflow routing fails.