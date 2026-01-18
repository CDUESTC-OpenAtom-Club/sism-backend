# 外键关系一致性验证报告

## 验证日期: 2026-01-17

## 验证范围
- Entity 目录: `sism-backend/src/main/java/com/sism/entity/`
- Schema 文件: `strategic-task-management/database/init.sql`

## 验证方法
检查每个 Entity 中的 `@ManyToOne` 注解与数据库表中的 `REFERENCES` 外键约束是否一致。

---

## 外键关系对照表

### 1. org 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| parentOrg | `@ManyToOne` `@JoinColumn(name = "parent_org_id")` | `parent_org_id BIGINT REFERENCES org(org_id)` | ✅ 一致 |

**自引用关系**: Org → Org (parent_org_id)

---

### 2. app_user 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| org | `@ManyToOne` `@JoinColumn(name = "org_id", nullable = false)` | `org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |

---

### 3. assessment_cycle 表

无外键关系。

---

### 4. strategic_task 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| cycle | `@ManyToOne` `@JoinColumn(name = "cycle_id", nullable = false)` | `cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id)` | ✅ 一致 |
| org | `@ManyToOne` `@JoinColumn(name = "org_id", nullable = false)` | `org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |
| createdByOrg | `@ManyToOne` `@JoinColumn(name = "created_by_org_id", nullable = false)` | `created_by_org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |

---

### 5. indicator 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| task | `@ManyToOne` `@JoinColumn(name = "task_id", nullable = false)` | `task_id BIGINT NOT NULL REFERENCES strategic_task(task_id)` | ✅ 一致 |
| parentIndicator | `@ManyToOne` `@JoinColumn(name = "parent_indicator_id")` | `parent_indicator_id BIGINT REFERENCES indicator(indicator_id)` | ✅ 一致 |
| ownerOrg | `@ManyToOne` `@JoinColumn(name = "owner_org_id", nullable = false)` | `owner_org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |
| targetOrg | `@ManyToOne` `@JoinColumn(name = "target_org_id", nullable = false)` | `target_org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |

**自引用关系**: Indicator → Indicator (parent_indicator_id)

---

### 6. milestone 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| indicator | `@ManyToOne` `@JoinColumn(name = "indicator_id", nullable = false)` | `indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id)` | ✅ 一致 |
| inheritedFrom | `@ManyToOne` `@JoinColumn(name = "inherited_from")` | `inherited_from BIGINT REFERENCES milestone(milestone_id)` | ✅ 一致 |

**自引用关系**: Milestone → Milestone (inherited_from)

---

### 7. progress_report 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| indicator | `@ManyToOne` `@JoinColumn(name = "indicator_id", nullable = false)` | `indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id)` | ✅ 一致 |
| milestone | `@ManyToOne` `@JoinColumn(name = "milestone_id")` | `milestone_id BIGINT REFERENCES milestone(milestone_id)` | ✅ 一致 |
| adhocTask | `@ManyToOne` `@JoinColumn(name = "adhoc_task_id")` | `adhoc_task_id BIGINT` + `ALTER TABLE ... ADD CONSTRAINT fk_report_adhoc FOREIGN KEY (adhoc_task_id) REFERENCES adhoc_task(adhoc_task_id)` | ✅ 一致 |
| reporter | `@ManyToOne` `@JoinColumn(name = "reporter_id", nullable = false)` | `reporter_id BIGINT NOT NULL REFERENCES app_user(user_id)` | ✅ 一致 |

**注意**: adhoc_task_id 的外键约束通过 ALTER TABLE 语句添加（因为 adhoc_task 表在 progress_report 之后创建）

---

### 8. approval_record 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| report | `@ManyToOne` `@JoinColumn(name = "report_id", nullable = false)` | `report_id BIGINT NOT NULL REFERENCES progress_report(report_id)` | ✅ 一致 |
| approver | `@ManyToOne` `@JoinColumn(name = "approver_id", nullable = false)` | `approver_id BIGINT NOT NULL REFERENCES app_user(user_id)` | ✅ 一致 |

---

### 9. alert_window 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| cycle | `@ManyToOne` `@JoinColumn(name = "cycle_id", nullable = false)` | `cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id)` | ✅ 一致 |

---

### 10. alert_rule 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| cycle | `@ManyToOne` `@JoinColumn(name = "cycle_id", nullable = false)` | `cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id)` | ✅ 一致 |

---

### 11. alert_event 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| indicator | `@ManyToOne` `@JoinColumn(name = "indicator_id", nullable = false)` | `indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id)` | ✅ 一致 |
| window | `@ManyToOne` `@JoinColumn(name = "window_id", nullable = false)` | `window_id BIGINT NOT NULL REFERENCES alert_window(window_id)` | ✅ 一致 |
| rule | `@ManyToOne` `@JoinColumn(name = "rule_id", nullable = false)` | `rule_id BIGINT NOT NULL REFERENCES alert_rule(rule_id)` | ✅ 一致 |
| handledBy | `@ManyToOne` `@JoinColumn(name = "handled_by")` | `handled_by BIGINT REFERENCES app_user(user_id)` | ✅ 一致 |

---

### 12. adhoc_task 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| cycle | `@ManyToOne` `@JoinColumn(name = "cycle_id", nullable = false)` | `cycle_id BIGINT NOT NULL REFERENCES assessment_cycle(cycle_id)` | ✅ 一致 |
| creatorOrg | `@ManyToOne` `@JoinColumn(name = "creator_org_id", nullable = false)` | `creator_org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |
| indicator | `@ManyToOne` `@JoinColumn(name = "indicator_id")` | `indicator_id BIGINT REFERENCES indicator(indicator_id)` | ✅ 一致 |

---

### 13. adhoc_task_target 表 (复合主键)

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| adhocTask | `@ManyToOne` `@JoinColumn(name = "adhoc_task_id", nullable = false)` | `adhoc_task_id BIGINT NOT NULL REFERENCES adhoc_task(adhoc_task_id)` | ✅ 一致 |
| targetOrg | `@ManyToOne` `@JoinColumn(name = "target_org_id", nullable = false)` | `target_org_id BIGINT NOT NULL REFERENCES org(org_id)` | ✅ 一致 |

**复合主键**: `PRIMARY KEY (adhoc_task_id, target_org_id)`

---

### 14. adhoc_task_indicator_map 表 (复合主键)

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| adhocTask | `@ManyToOne` `@JoinColumn(name = "adhoc_task_id", nullable = false)` | `adhoc_task_id BIGINT NOT NULL REFERENCES adhoc_task(adhoc_task_id)` | ✅ 一致 |
| indicator | `@ManyToOne` `@JoinColumn(name = "indicator_id", nullable = false)` | `indicator_id BIGINT NOT NULL REFERENCES indicator(indicator_id)` | ✅ 一致 |

**复合主键**: `PRIMARY KEY (adhoc_task_id, indicator_id)`

---

### 15. audit_log 表

| Entity 字段 | JPA 注解 | DB 外键约束 | 状态 |
|------------|---------|------------|------|
| actorUser | `@ManyToOne` `@JoinColumn(name = "actor_user_id")` | `actor_user_id BIGINT REFERENCES app_user(user_id)` | ✅ 一致 |
| actorOrg | `@ManyToOne` `@JoinColumn(name = "actor_org_id")` | `actor_org_id BIGINT REFERENCES org(org_id)` | ✅ 一致 |

---

## @OneToMany 关系验证

以下是 Entity 中定义的 `@OneToMany` 关系，这些是反向映射，不需要数据库外键约束：

| Entity | 字段 | mappedBy | 对应 @ManyToOne |
|--------|------|----------|-----------------|
| Org | childOrgs | parentOrg | Org.parentOrg ✅ |
| Indicator | childIndicators | parentIndicator | Indicator.parentIndicator ✅ |
| Indicator | milestones | indicator | Milestone.indicator ✅ |

---

## 外键关系统计

### 按表统计

| 表名 | @ManyToOne 数量 | DB FK 数量 | 状态 |
|-----|----------------|-----------|------|
| org | 1 | 1 | ✅ |
| app_user | 1 | 1 | ✅ |
| assessment_cycle | 0 | 0 | ✅ |
| strategic_task | 3 | 3 | ✅ |
| indicator | 4 | 4 | ✅ |
| milestone | 2 | 2 | ✅ |
| progress_report | 4 | 4 | ✅ |
| approval_record | 2 | 2 | ✅ |
| alert_window | 1 | 1 | ✅ |
| alert_rule | 1 | 1 | ✅ |
| alert_event | 4 | 4 | ✅ |
| adhoc_task | 3 | 3 | ✅ |
| adhoc_task_target | 2 | 2 | ✅ |
| adhoc_task_indicator_map | 2 | 2 | ✅ |
| audit_log | 2 | 2 | ✅ |
| **总计** | **32** | **32** | ✅ |

### 按目标表统计

| 被引用表 | 引用次数 |
|---------|---------|
| org | 9 |
| assessment_cycle | 4 |
| indicator | 5 |
| milestone | 2 |
| app_user | 4 |
| progress_report | 1 |
| adhoc_task | 3 |
| alert_window | 1 |
| alert_rule | 1 |
| strategic_task | 1 |

---

## 自引用关系

| 表 | 外键列 | 说明 |
|---|-------|------|
| org | parent_org_id | 组织层级结构 |
| indicator | parent_indicator_id | 指标层级结构 |
| milestone | inherited_from | 里程碑继承关系 |

---

## 可空性验证

### 必填外键 (NOT NULL)

| 表 | 外键列 | Entity nullable |
|---|-------|-----------------|
| app_user | org_id | false ✅ |
| strategic_task | cycle_id | false ✅ |
| strategic_task | org_id | false ✅ |
| strategic_task | created_by_org_id | false ✅ |
| indicator | task_id | false ✅ |
| indicator | owner_org_id | false ✅ |
| indicator | target_org_id | false ✅ |
| milestone | indicator_id | false ✅ |
| progress_report | indicator_id | false ✅ |
| progress_report | reporter_id | false ✅ |
| approval_record | report_id | false ✅ |
| approval_record | approver_id | false ✅ |
| alert_window | cycle_id | false ✅ |
| alert_rule | cycle_id | false ✅ |
| alert_event | indicator_id | false ✅ |
| alert_event | window_id | false ✅ |
| alert_event | rule_id | false ✅ |
| adhoc_task | cycle_id | false ✅ |
| adhoc_task | creator_org_id | false ✅ |
| adhoc_task_target | adhoc_task_id | false ✅ |
| adhoc_task_target | target_org_id | false ✅ |
| adhoc_task_indicator_map | adhoc_task_id | false ✅ |
| adhoc_task_indicator_map | indicator_id | false ✅ |

### 可选外键 (NULLABLE)

| 表 | 外键列 | Entity nullable |
|---|-------|-----------------|
| org | parent_org_id | true (无 nullable 属性) ✅ |
| indicator | parent_indicator_id | true (无 nullable 属性) ✅ |
| milestone | inherited_from | true (无 nullable 属性) ✅ |
| progress_report | milestone_id | true (无 nullable 属性) ✅ |
| progress_report | adhoc_task_id | true (无 nullable 属性) ✅ |
| alert_event | handled_by | true (无 nullable 属性) ✅ |
| adhoc_task | indicator_id | true (无 nullable 属性) ✅ |
| audit_log | actor_user_id | true (无 nullable 属性) ✅ |
| audit_log | actor_org_id | true (无 nullable 属性) ✅ |

---

## 验证结论

### ✅ 所有外键关系完全一致

1. **外键数量**: Entity 中的 32 个 @ManyToOne 关系与数据库中的 32 个外键约束完全对应
2. **列名映射**: 所有 @JoinColumn 的 name 属性与数据库外键列名一致
3. **目标表**: 所有外键引用的目标表与 Entity 关联的类型一致
4. **可空性**: 所有 nullable 属性与数据库 NOT NULL 约束一致
5. **自引用**: 3 个自引用关系（org, indicator, milestone）正确定义
6. **复合主键**: 2 个关联表（adhoc_task_target, adhoc_task_indicator_map）的复合主键正确定义

### 验证通过

**Requirements 2.2: ✅ 外键关系一致性验证通过**

