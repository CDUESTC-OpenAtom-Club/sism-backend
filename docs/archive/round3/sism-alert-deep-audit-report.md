---
name: sism-alert deep audit report
description: Third-round deep audit report for sism-alert module
type: audit
---

# Third-Round Deep Audit Report: sism-alert Module

## Finding 1 -- CRITICAL: Entire Module Not Registered in Spring (Module is Dead Code)

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-main/src/main/java/com/sism/main/SismMainApplication.java`, lines 29-46

**Severity:** Critical
**Category:** Bug / Architecture

**Evidence:**
```java
@SpringBootApplication(scanBasePackages = {"com.sism.iam", "com.sism.organization",
                                   "com.sism.strategy", "com.sism.task",
                                   "com.sism.workflow", "com.sism.execution",
                                   "com.sism.analytics",
                                   "com.sism.shared", "com.sism.config",
                                   "com.sism.exception", "com.sism.common",
                                   "com.sism.main"})
...
@ComponentScan(basePackages = {"com.sism.iam", "com.sism.organization",
                               "com.sism.strategy", "com.sism.task",
                               "com.sism.workflow", "com.sism.execution",
                               "com.sism.analytics",
                               "com.sism.shared", "com.sism.config",
                               "com.sism.exception", "com.sism.common",
                               "com.sism.main"})
```

Both `scanBasePackages` and `@ComponentScan` are explicit lists. `"com.sism.alert"` is absent from both. The only annotations that use wildcards are `@EntityScan("com.sism.**.domain")` and `@EnableJpaRepositories("com.sism.**.infrastructure.persistence")`, which means:
- The `Alert` JPA entity **will** be discovered.
- `JpaAlertRepository` **will** be discovered.
- `AlertApplicationService` and `AlertController` **will NOT** be Spring beans.

**Impact:** The entire `/api/v1/alerts` REST API is non-functional at runtime. All 10 endpoints return 404. No alert business logic executes. This is the single most impactful issue in the module.

---

## Finding 2 -- CRITICAL: Status Value Mismatch Between Code and Database CHECK Constraint

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-main/src/main/resources/db/migration/V1__baseline_current_schema.sql`, line 354
**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/domain/Alert.java`, lines 22-24

**Severity:** Critical
**Category:** Bug

**Evidence:**

Database CHECK constraint:
```sql
CONSTRAINT alert_event_status_check CHECK (((status)::text = ANY (ARRAY[('OPEN'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('RESOLVED'::character varying)::text, ('CLOSED'::character varying)::text])))
```

Code status constants:
```java
public static final String STATUS_PENDING = "PENDING";
public static final String STATUS_TRIGGERED = "TRIGGERED";
public static final String STATUS_RESOLVED = "RESOLVED";
```

No later migration (checked V1.9 and all others) modifies this constraint. The allowed DB values are `OPEN, IN_PROGRESS, RESOLVED, CLOSED`. The code uses `PENDING, TRIGGERED, RESOLVED`.

**Impact:** Even after the scanning issue is fixed, `createAlert()` sets status to `"PENDING"` which will violate the CHECK constraint. `triggerAlert()` sets status to `"TRIGGERED"` which also violates it. Both operations will throw a PostgreSQL constraint violation at runtime. Only `"RESOLVED"` happens to match.

---

## Finding 3 -- CRITICAL: Severity Value Mismatch (Triple Inconsistency)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-main/src/main/resources/db/migration/V1__baseline_current_schema.sql`, line 353
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/domain/enums/AlertSeverity.java`, lines 7-21
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/application/AlertApplicationService.java`, lines 115-120
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/rest/AlertController.java`, lines 311-313
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/dto/AlertRequest.java`, line 28

**Severity:** Critical
**Category:** Bug

**Evidence:**

| Source | Values Used |
|--------|-------------|
| DB `alert_event_severity_check` | `INFO, WARNING, CRITICAL` |
| `AlertSeverity` enum | `INFO, WARNING, CRITICAL` |
| `AlertApplicationService.getAlertStats()` | `CRITICAL, MAJOR, MINOR` |
| `AlertController.buildAlertStats()` | `CRITICAL, MAJOR, MINOR` |
| `AlertRequest` Swagger description | `"CRITICAL, MAJOR, MINOR"` |

Three separate severity vocabularies exist. The DB and the enum agree, but the application service and controller use completely different values (`MAJOR, MINOR`). The AlertRequest Swagger doc misleads API consumers into using values that will fail at the DB level.

**Impact:** Creating an alert with severity `"MAJOR"` or `"MINOR"` violates the DB CHECK constraint. The stats endpoints always report 0 for CRITICAL/MAJOR/MINOR because existing DB data (if any) uses INFO/WARNING/CRITICAL.

---

## Finding 4 -- HIGH: N+1 Query Problem in Permission Filtering

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/rest/AlertController.java`, lines 236-243 and 250-263

**Severity:** High
**Category:** Performance

**Evidence:**
```java
private List<Alert> filterAlertsByPermission(List<Alert> alerts, Authentication authentication) {
    if (isAdmin(authentication)) { return alerts; }
    return alerts.stream()
            .filter(alert -> alert != null && alert.getIndicatorId() != null
                    && hasIndicatorAccess(alert.getIndicatorId(), authentication))
            .toList();
}

private boolean hasIndicatorAccess(Long indicatorId, Authentication authentication) {
    // ...
    return indicatorRepository.findById(indicatorId)
            .filter(indicator -> ownsOrTargetsIndicator(indicator, currentUser.getOrgId()))
            .isPresent();
}
```

This is used by 6 out of 10 endpoints: `getAllAlerts`, `getAlertsByStatus`, `getAlertsBySeverity`, `getAlertsByIndicator`, `getUnresolvedAlerts`, `getUnclosedAlertEvents`, `countAlerts`, and `getAlertStats`. Every non-admin call loads all alerts then fires one `indicatorRepository.findById()` per alert.

**Impact:** With 1000 alerts, a non-admin user triggers 1000+ sequential DB queries for permission checks. This will cause severe latency and potential connection pool exhaustion under load.

---

## Finding 5 -- HIGH: DB NOT NULL Constraints vs Optional DTO Fields

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-main/src/main/resources/db/migration/V1__baseline_current_schema.sql`, lines 350-352
**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/dto/AlertRequest.java`

**Severity:** High
**Category:** Bug

**Evidence:**

Database DDL:
```sql
indicator_id bigint NOT NULL,
rule_id bigint NOT NULL,
window_id bigint NOT NULL
-- Also: actual_percent numeric(5,2) NOT NULL, expected_percent numeric(5,2) NOT NULL, gap_percent numeric(5,2) NOT NULL
```

AlertRequest DTO:
```java
@NotNull(message = "指标ID不能为空")  // Only indicatorId is required
private Long indicatorId;

private Long ruleId;        // No @NotNull - optional in DTO
private Long windowId;      // No @NotNull - optional in DTO
private BigDecimal actualPercent;    // No validation
private BigDecimal expectedPercent;   // No validation
private BigDecimal gapPercent;        // No validation
```

**Impact:** An API consumer can omit `ruleId`, `windowId`, `actualPercent`, `expectedPercent`, or `gapPercent`. The request passes DTO validation, proceeds to `alertRepository.save()`, and then JPA attempts an INSERT with NULL values for NOT NULL columns. This results in a raw SQL constraint violation exception with no user-friendly error message.

---

## Finding 6 -- HIGH: `detail_json` Column Type Mismatch Between JPA and Database

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/domain/Alert.java`, line 50-51
**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-main/src/main/resources/db/migration/V1__baseline_current_schema.sql`, line 343

**Severity:** High
**Category:** Bug

**Evidence:**

Database: `detail_json jsonb`
Code: `@Column(name = "detail_json", columnDefinition = "TEXT") private String detailJson;`

**Impact:** The `columnDefinition = "TEXT"` tells Hibernate to expect a TEXT column, but the actual DB column is `jsonb`. While PostgreSQL can implicitly cast between TEXT and JSONB for simple inserts, this will cause issues with: (a) schema validation tools that verify column definitions, (b) any future native queries that rely on JSONB operators, (c) potential data loss if the TEXT path truncates or escapes JSON content differently than JSONB storage.

---

## Finding 7 -- MEDIUM: No Status Transition Guards (Invalid State Machine)

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/domain/Alert.java`, lines 86-96

**Severity:** Medium
**Category:** Bug

**Evidence:**
```java
public void trigger() {
    this.status = STATUS_TRIGGERED;
    this.updatedAt = LocalDateTime.now();
}

public void resolve(Long handledBy, String handledNote) {
    this.status = STATUS_RESOLVED;
    this.handledBy = handledBy;
    this.handledNote = handledNote;
    this.updatedAt = LocalDateTime.now();
}
```

Neither method checks the current status. Possible invalid transitions:
- `trigger()` on an already TRIGGERED alert (no-op, but semantically wrong)
- `trigger()` on a RESOLVED alert (reopens it silently)
- `resolve()` on a PENDING alert (skips the TRIGGERED state entirely)
- `resolve()` on an already RESOLVED alert (overwrites handler info)

**Impact:** Data inconsistencies in alert state. No business rules enforce valid state transitions.

---

## Finding 8 -- MEDIUM: AlertWindow Entity Is Missing (Dead Code Reference)

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/domain/Alert.java`, lines 112-118

**Severity:** Medium
**Category:** Bug / Code Quality

**Evidence:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "window_id")
private AlertWindow alertWindow;  // AlertWindow does NOT exist in source!
```

The import `import com.sism.alert.domain.AlertWindow;` is missing and there is no file for this class in the module. This is a reference to a dead entity.

**Impact:** Any attempt to access `alert.getAlertWindow()` will throw a `ClassNotFoundException` at runtime. The field is currently not used in any production code, but it's a ticking time bomb.

---

## Finding 9 -- MEDIUM: No Alert Deletion API Endpoint

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/rest/AlertController.java` (entire file)

**Severity:** Medium
**Category:** Feature Gap / API Completeness

**Evidence:** There is no `DELETE /api/v1/alerts/{id}` endpoint. Alerts can only be resolved, not deleted.

**Impact:** The system lacks a way to remove stale or test alerts. Over time, the `alert_events` table will accumulate a large number of resolved alerts that can never be removed.

---

## Finding 10 -- MEDIUM: `getAlertStats()` Loads ALL Alerts Just to Count Them

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/rest/AlertController.java`, lines 311-343

**Severity:** Medium
**Category:** Performance

**Evidence:**
```java
public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertStats() {
    List<Alert> alerts = alertRepository.findAll();
    long totalAlerts = alerts.size();
    long unresolvedAlerts = alerts.stream()
            .filter(alert -> !STATUS_RESOLVED.equals(alert.getStatus()))
            .count();
    // ...
}
```

**Impact:** For 100,000 alerts, this loads every alert into memory just to count them. A proper implementation would use `COUNT(*)` queries with `WHERE` clauses.

---

## Finding 11 -- MEDIUM: No Caching on Any Alert Endpoints

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/rest/AlertController.java` (all methods)

**Severity:** Medium
**Category:** Performance

**Evidence:** No endpoints have `@Cacheable` or `@CacheEvict` annotations. The `getAllAlerts`, `getAlertStats`, and `getUnresolvedAlerts` endpoints are called frequently but always hit the database.

**Impact:** Under high load, these endpoints will cause significant DB pressure.

---

## Finding 12 -- MEDIUM: extractUserId Assumes Username Is Always Numeric

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/application/AlertApplicationService.java`, lines 143-150

**Severity:** Medium
**Category:** Bug / Robustness

**Evidence:**
```java
private Long extractUserId(String username) {
    try {
        return Long.valueOf(username);
    } catch (NumberFormatException e) {
        log.warn("Cannot extract user ID from username: {}", username);
        return null;
    }
}
```

**Impact:** If usernames ever contain non-numeric characters, the system will fail to extract user IDs for alert ownership and handling.

---

## Finding 13 -- MEDIUM: `getUnresolvedAlerts` Returns Only Pending (Not Triggered)

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/application/AlertApplicationService.java`, lines 42-47

**Severity:** Medium
**Category:** Bug / Business Logic

**Evidence:**
```java
@Override
public List<Alert> getUnresolvedAlerts() {
    return alertRepository.findByStatusNot(STATUS_RESOLVED);
}
```

This returns both `PENDING` and `TRIGGERED` alerts, which is correct. However, the method name and Javadoc imply it should only return `PENDING` (untriggered) alerts.

---

## Finding 14 -- LOW: AlertRequest Lacks `@Valid` on Nested Fields

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/dto/AlertRequest.java`, line 1

**Severity:** Low
**Category:** Validation

**Evidence:** The DTO has `@NotNull` on top-level fields but no `@Valid` for nested objects (though there are none in this case).

---

## Finding 15 -- LOW: No Domain Events Published for Alert Lifecycle

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/domain/Alert.java` (entire file)

**Severity:** Low
**Category:** Architecture

**Evidence:** `Alert` extends `AggregateRoot` but never calls `registerEvent()` or `addEvent()`. No domain events are published for alert creation, triggering, or resolution.

---

## Finding 16 -- LOW: AlertResponse Exposes All Internal Fields

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-alert/src/main/java/com/sism/alert/interfaces/response/AlertResponse.java`, lines 1-33

**Severity:** Low
**Category:** API Design

**Evidence:** The response DTO has fields like `id`, `indicatorId`, `ruleId`, `windowId`, `status`, `severity`, `actualPercent`, `expectedPercent`, `gapPercent`, `detailJson`, `handledBy`, `handledNote`, `createdAt`, `updatedAt`. There is no filtering or redaction.

**Impact:** Sensitive information like `detailJson` (which may contain indicator data) is exposed to all users with access to the alert.

---

## Summary Table

| # | Finding | Severity | Category | File(s) |
|---|---------|----------|----------|---------|
| 1 | Module not registered in Spring | CRITICAL | Bug/Architecture | SismMainApplication.java:29 |
| 2 | DB CHECK constraint mismatch (status) | CRITICAL | Bug | V1__*.sql:354, Alert.java:22-24 |
| 3 | Severity value triple inconsistency | CRITICAL | Bug | V1__*.sql:353, AlertSeverity.java:7-21, AlertApplicationService.java:115-120, AlertController.java:311-313, AlertRequest.java:28 |
| 4 | N+1 query in permission filtering | HIGH | Performance | AlertController.java:236-243, 250-263 |
| 5 | DB NOT NULL columns missing DTO validation | HIGH | Bug | V1__*.sql:350-352, AlertRequest.java |
| 6 | detail_json column type mismatch (TEXT vs jsonb) | HIGH | Bug | Alert.java:50-51, V1__*.sql:343 |
| 7 | No status transition guards (invalid state machine) | MEDIUM | Bug | Alert.java:86-96 |
| 8 | AlertWindow entity is missing (dead reference) | MEDIUM | Bug/Code Quality | Alert.java:112-118 |
| 9 | No alert deletion API endpoint | MEDIUM | Feature Gap/API Completeness | AlertController.java (entire file) |
| 10 | getAlertStats() loads all alerts just to count | MEDIUM | Performance | AlertController.java:311-343 |
| 11 | No caching on any endpoints | MEDIUM | Performance | AlertController.java (all methods) |
| 12 | extractUserId assumes numeric usernames | MEDIUM | Bug/Robustness | AlertApplicationService.java:143-150 |
| 13 | getUnresolvedAlerts returns both pending and triggered | MEDIUM | Bug/Business Logic | AlertApplicationService.java:42-47 |
| 14 | AlertRequest lacks @Valid on nested fields | LOW | Validation | AlertRequest.java:1 |
| 15 | No domain events published | LOW | Architecture | Alert.java (entire file) |
| 16 | AlertResponse exposes all internal fields | LOW | API Design | AlertResponse.java:1-33