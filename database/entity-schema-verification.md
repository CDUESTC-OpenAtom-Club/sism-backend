# Entity-Schema 字段对应验证报告

## 验证日期: 2026-01-17

## 验证范围
- Entity 目录: `sism-backend/src/main/java/com/sism/entity/`
- Schema 文件: `strategic-task-management/database/init.sql`

---

## 1. org 表 ↔ Org Entity

### 数据库表定义
```sql
CREATE TABLE org (
    org_id BIGSERIAL PRIMARY KEY,
    org_name VARCHAR(100) NOT NULL,
    org_type org_type NOT NULL,
    parent_org_id BIGINT REFERENCES org(org_id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| org_id | orgId | Long | ✅ |
| org_name | orgName | String | ✅ |
| org_type | orgType | OrgType (Enum) | ✅ |
| parent_org_id | parentOrg | Org (FK) | ✅ |
| is_active | isActive | Boolean | ✅ |
| sort_order | sortOrder | Integer | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 2. app_user 表 ↔ AppUser Entity

### 数据库表定义
```sql
CREATE TABLE app_user (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    real_name VARCHAR(50) NOT NULL,
    org_id BIGINT NOT NULL REFERENCES org(org_id),
    password_hash VARCHAR(255) NOT NULL,
    sso_id VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| user_id | userId | Long | ✅ |
| username | username | String | ✅ |
| real_name | realName | String | ✅ |
| org_id | org | Org (FK) | ✅ |
| password_hash | passwordHash | String | ✅ |
| sso_id | ssoId | String | ✅ |
| is_active | isActive | Boolean | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 3. assessment_cycle 表 ↔ AssessmentCycle Entity

### 数据库表定义
```sql
CREATE TABLE assessment_cycle (
    cycle_id BIGSERIAL PRIMARY KEY,
    cycle_name VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| cycle_id | cycleId | Long | ✅ |
| cycle_name | cycleName | String | ✅ |
| year | year | Integer | ✅ |
| start_date | startDate | LocalDate | ✅ |
| end_date | endDate | LocalDate | ✅ |
| description | description | String | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 4. strategic_task 表 ↔ StrategicTask Entity

### 数据库表定义
```sql
CREATE TABLE strategic_task (
    task_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    task_name VARCHAR(200) NOT NULL,
    task_desc TEXT,
    task_type task_type NOT NULL DEFAULT 'BASIC',
    org_id BIGINT NOT NULL REFERENCES org(org_id),
    created_by_org_id BIGINT NOT NULL REFERENCES org(org_id),
    sort_order INT NOT NULL DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| task_id | taskId | Long | ✅ |
| cycle_id | cycle | AssessmentCycle (FK) | ✅ |
| task_name | taskName | String | ✅ |
| task_desc | taskDesc | String | ✅ |
| task_type | taskType | TaskType (Enum) | ✅ |
| org_id | org | Org (FK) | ✅ |
| created_by_org_id | createdByOrg | Org (FK) | ✅ |
| sort_order | sortOrder | Integer | ✅ |
| remark | remark | String | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 5. indicator 表 ↔ Indicator Entity

### 数据库表定义
```sql
CREATE TABLE indicator (
    indicator_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES strategic_task(task_id),
    parent_indicator_id BIGINT REFERENCES indicator(indicator_id),
    level indicator_level NOT NULL,
    owner_org_id BIGINT NOT NULL REFERENCES org(org_id),
    target_org_id BIGINT NOT NULL REFERENCES org(org_id),
    indicator_desc TEXT NOT NULL,
    weight_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    year INT NOT NULL,
    status indicator_status NOT NULL DEFAULT 'ACTIVE',
    remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| indicator_id | indicatorId | Long | ✅ |
| task_id | task | StrategicTask (FK) | ✅ |
| parent_indicator_id | parentIndicator | Indicator (FK) | ✅ |
| level | level | IndicatorLevel (Enum) | ✅ |
| owner_org_id | ownerOrg | Org (FK) | ✅ |
| target_org_id | targetOrg | Org (FK) | ✅ |
| indicator_desc | indicatorDesc | String | ✅ |
| weight_percent | weightPercent | BigDecimal | ✅ |
| sort_order | sortOrder | Integer | ✅ |
| year | year | Integer | ✅ |
| status | status | IndicatorStatus (Enum) | ✅ |
| remark | remark | String | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 6. milestone 表 ↔ Milestone Entity

### 数据库表定义
```sql
CREATE TABLE milestone (
    milestone_id BIGSERIAL PRIMARY KEY,
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    milestone_name VARCHAR(200) NOT NULL,
    milestone_desc TEXT,
    due_date DATE NOT NULL,
    weight_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    status milestone_status NOT NULL DEFAULT 'NOT_STARTED',
    sort_order INT NOT NULL DEFAULT 0,
    inherited_from BIGINT REFERENCES milestone(milestone_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| milestone_id | milestoneId | Long | ✅ |
| indicator_id | indicator | Indicator (FK) | ✅ |
| milestone_name | milestoneName | String | ✅ |
| milestone_desc | milestoneDesc | String | ✅ |
| due_date | dueDate | LocalDate | ✅ |
| weight_percent | weightPercent | BigDecimal | ✅ |
| status | status | MilestoneStatus (Enum) | ✅ |
| sort_order | sortOrder | Integer | ✅ |
| inherited_from | inheritedFrom | Milestone (FK) | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 7. progress_report 表 ↔ ProgressReport Entity

### 数据库表定义
```sql
CREATE TABLE progress_report (
    report_id BIGSERIAL PRIMARY KEY,
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    milestone_id BIGINT REFERENCES milestone(milestone_id),
    adhoc_task_id BIGINT REFERENCES adhoc_task(adhoc_task_id),
    percent_complete NUMERIC(5,2) NOT NULL DEFAULT 0,
    achieved_milestone BOOLEAN NOT NULL DEFAULT FALSE,
    narrative TEXT,
    reporter_id BIGINT NOT NULL REFERENCES app_user(user_id),
    status report_status NOT NULL DEFAULT 'DRAFT',
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    version_no INT NOT NULL DEFAULT 1,
    reported_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| report_id | reportId | Long | ✅ |
| indicator_id | indicator | Indicator (FK) | ✅ |
| milestone_id | milestone | Milestone (FK) | ✅ |
| adhoc_task_id | adhocTask | AdhocTask (FK) | ✅ |
| percent_complete | percentComplete | BigDecimal | ✅ |
| achieved_milestone | achievedMilestone | Boolean | ✅ |
| narrative | narrative | String | ✅ |
| reporter_id | reporter | AppUser (FK) | ✅ |
| status | status | ReportStatus (Enum) | ✅ |
| is_final | isFinal | Boolean | ✅ |
| version_no | versionNo | Integer | ✅ |
| reported_at | reportedAt | LocalDateTime | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 8. approval_record 表 ↔ ApprovalRecord Entity

### 数据库表定义
```sql
CREATE TABLE approval_record (
    approval_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES progress_report(report_id),
    approver_id BIGINT NOT NULL REFERENCES app_user(user_id),
    action approval_action NOT NULL,
    comment TEXT,
    acted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| approval_id | approvalId | Long | ✅ |
| report_id | report | ProgressReport (FK) | ✅ |
| approver_id | approver | AppUser (FK) | ✅ |
| action | action | ApprovalAction (Enum) | ✅ |
| comment | comment | String | ✅ |
| acted_at | actedAt | LocalDateTime | ✅ |

**注意**: ApprovalRecord 不继承 BaseEntity，无 created_at/updated_at 字段，与数据库表一致。

**结论: ✅ 完全匹配**

---

## 9. alert_window 表 ↔ AlertWindow Entity

### 数据库表定义
```sql
CREATE TABLE alert_window (
    window_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    name VARCHAR(100) NOT NULL,
    cutoff_date DATE NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| window_id | windowId | Long | ✅ |
| cycle_id | cycle | AssessmentCycle (FK) | ✅ |
| name | name | String | ✅ |
| cutoff_date | cutoffDate | LocalDate | ✅ |
| is_default | isDefault | Boolean | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 10. alert_rule 表 ↔ AlertRule Entity

### 数据库表定义
```sql
CREATE TABLE alert_rule (
    rule_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    name VARCHAR(100) NOT NULL,
    severity alert_severity NOT NULL DEFAULT 'WARNING',
    gap_threshold NUMERIC(5,2) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| rule_id | ruleId | Long | ✅ |
| cycle_id | cycle | AssessmentCycle (FK) | ✅ |
| name | name | String | ✅ |
| severity | severity | AlertSeverity (Enum) | ✅ |
| gap_threshold | gapThreshold | BigDecimal | ✅ |
| is_enabled | isEnabled | Boolean | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 11. alert_event 表 ↔ AlertEvent Entity

### 数据库表定义
```sql
CREATE TABLE alert_event (
    event_id BIGSERIAL PRIMARY KEY,
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    window_id BIGINT NOT NULL REFERENCES alert_window(window_id),
    rule_id BIGINT NOT NULL REFERENCES alert_rule(rule_id),
    expected_percent NUMERIC(5,2) NOT NULL,
    actual_percent NUMERIC(5,2) NOT NULL,
    gap_percent NUMERIC(5,2) NOT NULL,
    severity alert_severity NOT NULL,
    status alert_status NOT NULL DEFAULT 'OPEN',
    handled_by BIGINT REFERENCES app_user(user_id),
    handled_note TEXT,
    detail_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| event_id | eventId | Long | ✅ |
| indicator_id | indicator | Indicator (FK) | ✅ |
| window_id | window | AlertWindow (FK) | ✅ |
| rule_id | rule | AlertRule (FK) | ✅ |
| expected_percent | expectedPercent | BigDecimal | ✅ |
| actual_percent | actualPercent | BigDecimal | ✅ |
| gap_percent | gapPercent | BigDecimal | ✅ |
| severity | severity | AlertSeverity (Enum) | ✅ |
| status | status | AlertStatus (Enum) | ✅ |
| handled_by | handledBy | AppUser (FK) | ✅ |
| handled_note | handledNote | String | ✅ |
| detail_json | detailJson | Map<String, Object> | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 12. adhoc_task 表 ↔ AdhocTask Entity

### 数据库表定义
```sql
CREATE TABLE adhoc_task (
    adhoc_task_id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id),
    creator_org_id BIGINT NOT NULL REFERENCES org(org_id),
    scope_type adhoc_scope_type NOT NULL DEFAULT 'ALL_ORGS',
    indicator_id BIGINT REFERENCES indicator(indicator_id),
    task_title VARCHAR(200) NOT NULL,
    task_desc TEXT,
    open_at DATE,
    due_at DATE,
    include_in_alert BOOLEAN NOT NULL DEFAULT FALSE,
    require_indicator_report BOOLEAN NOT NULL DEFAULT FALSE,
    status adhoc_task_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| adhoc_task_id | adhocTaskId | Long | ✅ |
| cycle_id | cycle | AssessmentCycle (FK) | ✅ |
| creator_org_id | creatorOrg | Org (FK) | ✅ |
| scope_type | scopeType | AdhocScopeType (Enum) | ✅ |
| indicator_id | indicator | Indicator (FK) | ✅ |
| task_title | taskTitle | String | ✅ |
| task_desc | taskDesc | String | ✅ |
| open_at | openAt | LocalDate | ✅ |
| due_at | dueAt | LocalDate | ✅ |
| include_in_alert | includeInAlert | Boolean | ✅ |
| require_indicator_report | requireIndicatorReport | Boolean | ✅ |
| status | status | AdhocTaskStatus (Enum) | ✅ |
| created_at | createdAt | LocalDateTime | ✅ (BaseEntity) |
| updated_at | updatedAt | LocalDateTime | ✅ (BaseEntity) |

**结论: ✅ 完全匹配**

---

## 13. adhoc_task_target 表 ↔ AdhocTaskTarget Entity

### 数据库表定义
```sql
CREATE TABLE adhoc_task_target (
    adhoc_task_id BIGINT NOT NULL REFERENCES adhoc_task(adhoc_task_id),
    target_org_id BIGINT NOT NULL REFERENCES org(org_id),
    PRIMARY KEY (adhoc_task_id, target_org_id)
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| adhoc_task_id | adhocTask | AdhocTask (FK, PK) | ✅ |
| target_org_id | targetOrg | Org (FK, PK) | ✅ |

**注意**: 使用复合主键 `@IdClass(AdhocTaskTargetId.class)`

**结论: ✅ 完全匹配**

---

## 14. adhoc_task_indicator_map 表 ↔ AdhocTaskIndicatorMap Entity

### 数据库表定义
```sql
CREATE TABLE adhoc_task_indicator_map (
    adhoc_task_id BIGINT NOT NULL REFERENCES adhoc_task(adhoc_task_id),
    indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id),
    PRIMARY KEY (adhoc_task_id, indicator_id)
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| adhoc_task_id | adhocTask | AdhocTask (FK, PK) | ✅ |
| indicator_id | indicator | Indicator (FK, PK) | ✅ |

**注意**: 使用复合主键 `@IdClass(AdhocTaskIndicatorMapId.class)`

**结论: ✅ 完全匹配**

---

## 15. audit_log 表 ↔ AuditLog Entity

### 数据库表定义
```sql
CREATE TABLE audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    entity_type audit_entity_type NOT NULL,
    entity_id BIGINT NOT NULL,
    action audit_action NOT NULL,
    before_json JSONB,
    after_json JSONB,
    changed_fields JSONB,
    reason TEXT,
    actor_user_id BIGINT REFERENCES app_user(user_id),
    actor_org_id BIGINT REFERENCES org(org_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Entity 字段映射
| DB Column | Entity Field | Java Type | 状态 |
|-----------|--------------|-----------|------|
| log_id | logId | Long | ✅ |
| entity_type | entityType | AuditEntityType (Enum) | ✅ |
| entity_id | entityId | Long | ✅ |
| action | action | AuditAction (Enum) | ✅ |
| before_json | beforeJson | Map<String, Object> | ✅ |
| after_json | afterJson | Map<String, Object> | ✅ |
| changed_fields | changedFields | Map<String, Object> | ✅ |
| reason | reason | String | ✅ |
| actor_user_id | actorUser | AppUser (FK) | ✅ |
| actor_org_id | actorOrg | Org (FK) | ✅ |
| created_at | createdAt | LocalDateTime | ✅ |

**注意**: AuditLog 不继承 BaseEntity，只有 created_at 字段，与数据库表一致。

**结论: ✅ 完全匹配**

---

## 验证总结

### 表/Entity 对应关系

| # | 数据库表 | Entity 类 | 状态 |
|---|---------|-----------|------|
| 1 | org | Org | ✅ 完全匹配 |
| 2 | app_user | AppUser | ✅ 完全匹配 |
| 3 | assessment_cycle | AssessmentCycle | ✅ 完全匹配 |
| 4 | strategic_task | StrategicTask | ✅ 完全匹配 |
| 5 | indicator | Indicator | ✅ 完全匹配 |
| 6 | milestone | Milestone | ✅ 完全匹配 |
| 7 | progress_report | ProgressReport | ✅ 完全匹配 |
| 8 | approval_record | ApprovalRecord | ✅ 完全匹配 |
| 9 | alert_window | AlertWindow | ✅ 完全匹配 |
| 10 | alert_rule | AlertRule | ✅ 完全匹配 |
| 11 | alert_event | AlertEvent | ✅ 完全匹配 |
| 12 | adhoc_task | AdhocTask | ✅ 完全匹配 |
| 13 | adhoc_task_target | AdhocTaskTarget | ✅ 完全匹配 |
| 14 | adhoc_task_indicator_map | AdhocTaskIndicatorMap | ✅ 完全匹配 |
| 15 | audit_log | AuditLog | ✅ 完全匹配 |

### 类型映射验证

| Java 类型 | PostgreSQL 类型 | 状态 |
|-----------|-----------------|------|
| Long | BIGSERIAL/BIGINT | ✅ |
| String | VARCHAR/TEXT | ✅ |
| Integer | INT | ✅ |
| Boolean | BOOLEAN | ✅ |
| BigDecimal | NUMERIC(5,2) | ✅ |
| LocalDate | DATE | ✅ |
| LocalDateTime | TIMESTAMP | ✅ |
| Enum (EnumType.STRING) | PostgreSQL ENUM | ✅ |
| Map<String, Object> | JSONB | ✅ |

### 特殊设计说明

1. **BaseEntity 继承**: 大部分 Entity 继承 BaseEntity，自动获得 `created_at` 和 `updated_at` 字段
2. **例外情况**: 
   - `ApprovalRecord` 不继承 BaseEntity，只有 `acted_at` 字段
   - `AuditLog` 不继承 BaseEntity，只有 `created_at` 字段
3. **复合主键**: `AdhocTaskTarget` 和 `AdhocTaskIndicatorMap` 使用 `@IdClass` 实现复合主键
4. **JSONB 映射**: 使用 `@JdbcTypeCode(SqlTypes.JSON)` 映射 PostgreSQL JSONB 类型

### 最终结论

**✅ 所有 15 个 Entity 类与数据库表定义完全匹配**

- 字段名称: 全部正确映射
- 字段类型: 全部兼容
- 外键关系: 全部正确定义
- 约束条件: 全部一致

验证通过，可以进行下一步任务。
