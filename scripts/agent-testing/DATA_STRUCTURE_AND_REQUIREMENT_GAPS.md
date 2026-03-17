# SISM 数据结构不匹配与业务需求缺口分析报告

**分析时间**: 2026-03-17 10:41
**分析依据**:
- 业务流程文档: `docs/流程.md`
- 测试场景配置: `config/test-scenarios.json`
- API端点配置: `config/endpoints.json`

---

## 📊 执行摘要

| 类别 | 已覆盖 | 缺失 | 优先级 |
|------|--------|------|--------|
| 核心业务流程 | 5/5 | 0 | ✅ |
| 数据结构字段 | 待验证 | 3个关键字段 | ⚠️ P1 |
| API接口 | 43个 | 12个 | ⚠️ P1-P2 |
| UX关键功能 | 2/6 | 4个 | ⚠️ P1 |

---

## 🔍 数据结构验证清单

### 1. indicator 表 (战略指标表)

#### ✅ 已确认字段
- `status` - 状态字段 (DRAFT/PENDING/DISTRIBUTED)
- `parent_indicator_id` - 父子关系字段

#### ⚠️ 需要验证的字段
```sql
-- 必须验证以下字段是否存在
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'strategic_indicator'
  AND column_name IN (
    'cycle_id',           -- 周期标识 (必需)
    'progress',           -- 进度值 (必需)
    'weight',             -- 权重 (用于子指标)
    'level',              -- 指标层级 (可选)
    'archived_at',        -- 归档时间 (建议新增)
    'archived_status'     -- 归档状态 (建议新增)
  );
```

**业务影响**:
- ❌ 缺少 `cycle_id`: 无法区分不同周期的数据
- ❌ 缺少 `progress`: 无法更新指标进度
- ⚠️ 缺少归档字段: 用户无法证明"已完成"

---

### 2. plan_report / progress_report 表 (填报表)

#### ✅ 已确认字段
- `status` - 状态字段 (DRAFT/PENDING/APPROVED)

#### ⚠️ 需要验证的字段
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name IN ('plan_report', 'progress_report')
  AND column_name IN (
    'cycle_id',           -- 周期标识 (必需)
    'indicator_id',       -- 关联指标 (必需)
    'progress_value',     -- 填报进度值 (必需)
    'archived_at',        -- 归档时间 (建议新增)
    'version'             -- 版本号 (建议新增)
  );
```

**业务影响**:
- ❌ 缺少 `cycle_id`: 无法区分本周期与历史数据
- ⚠️ 缺少版本字段: 难以追溯历史修改

---

### 3. audit_instance 表 (审批实例表)

#### ✅ 已确认字段
- `status` - 审批状态
- `current_step_id` - 当前步骤

#### ⚠️ 需要验证的字段
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'audit_instance'
  AND column_name IN (
    'initiator_id',       -- 发起人 (必需)
    'requester_org_id',   -- 发起组织 (必需)
    'entity_type',        -- 实体类型 (必需)
    'entity_id',          -- 实体ID (必需)
    'agent_user_id'       -- 代理人 (后续功能)
  );
```

---

### 4. audit_step_instance 表 (审批步骤实例表)

#### ✅ 已确认字段
- `status` - 步骤状态
- `comment` - 审批意见

#### ⚠️ 需要验证的字段
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'audit_step_instance'
  AND column_name IN (
    'assignee_id',        -- 审批人 (必需)
    'executed_at',        -- 执行时间 (必需)
    'type',               -- 操作类型 (APPROVE/REJECT/WITHDRAW)
    'delegated_by',       -- 转办来源 (后续功能)
    'change_log'          -- 数据变更记录 (建议新增)
  );
```

**业务影响**:
- ⚠️ 缺少 `type` 字段: 无法区分撤回操作
- ⚠️ 缺少 `change_log`: 无法追踪数据变更

---

