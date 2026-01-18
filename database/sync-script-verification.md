# 数据同步脚本验证报告

## 验证日期
2026-01-17

## 验证范围
- `strategic-task-management/scripts/sync-all.js` - 主入口脚本
- `strategic-task-management/scripts/phases/sync-org.js` - 组织机构同步
- `strategic-task-management/scripts/phases/sync-cycle.js` - 考核周期同步
- `strategic-task-management/scripts/phases/sync-task.js` - 战略任务同步
- `strategic-task-management/scripts/phases/sync-indicator.js` - 指标同步
- `strategic-task-management/scripts/phases/sync-milestone.js` - 里程碑同步

## 字段映射验证

### 1. sync-org.js → Org Entity

| 脚本字段 | Entity 字段 | 数据库列 | 类型 | 状态 |
|---------|------------|---------|------|------|
| org_name | orgName | org_name | VARCHAR(100) | ✅ 匹配 |
| org_type | orgType | org_type | org_type ENUM | ✅ 匹配 |
| sort_order | sortOrder | sort_order | INT | ✅ 匹配 |

**枚举值验证:**
- `STRATEGY_DEPT` → OrgType.STRATEGY_DEPT ✅
- `FUNCTION_DEPT` → OrgType.FUNCTION_DEPT ✅
- `COLLEGE` → OrgType.COLLEGE ✅

### 2. sync-cycle.js → AssessmentCycle Entity

| 脚本字段 | Entity 字段 | 数据库列 | 类型 | 状态 |
|---------|------------|---------|------|------|
| cycle_name | cycleName | cycle_name | VARCHAR(100) | ✅ 匹配 |
| year | year | year | INT | ✅ 匹配 |
| start_date | startDate | start_date | DATE | ✅ 匹配 |
| end_date | endDate | end_date | DATE | ✅ 匹配 |
| description | description | description | TEXT | ✅ 匹配 |

### 3. sync-task.js → StrategicTask Entity

| 脚本字段 | Entity 字段 | 数据库列 | 类型 | 状态 |
|---------|------------|---------|------|------|
| cycle_id | cycle.cycleId | cycle_id | BIGINT FK | ✅ 匹配 |
| task_name | taskName | task_name | VARCHAR(200) | ✅ 匹配 |
| task_desc | taskDesc | task_desc | TEXT | ✅ 匹配 |
| task_type | taskType | task_type | task_type ENUM | ✅ 匹配 |
| org_id | org.orgId | org_id | BIGINT FK | ✅ 匹配 |
| created_by_org_id | createdByOrg.orgId | created_by_org_id | BIGINT FK | ✅ 匹配 |
| sort_order | sortOrder | sort_order | INT | ✅ 匹配 |

**枚举值验证:**
- `DEVELOPMENT` → TaskType.DEVELOPMENT ✅
- `BASIC` → TaskType.BASIC ✅

### 4. sync-indicator.js → Indicator Entity

| 脚本字段 | Entity 字段 | 数据库列 | 类型 | 状态 |
|---------|------------|---------|------|------|
| task_id | task.taskId | task_id | BIGINT FK | ✅ 匹配 |
| indicator_desc | indicatorDesc | indicator_desc | TEXT | ✅ 匹配 |
| level | level | level | indicator_level ENUM | ✅ 匹配 |
| weight_percent | weightPercent | weight_percent | NUMERIC(5,2) | ✅ 匹配 |
| status | status | status | indicator_status ENUM | ✅ 匹配 |
| year | year | year | INT | ✅ 匹配 |
| owner_org_id | ownerOrg.orgId | owner_org_id | BIGINT FK | ✅ 匹配 |
| target_org_id | targetOrg.orgId | target_org_id | BIGINT FK | ✅ 匹配 |
| parent_indicator_id | parentIndicator.indicatorId | parent_indicator_id | BIGINT FK | ✅ 匹配 |
| sort_order | sortOrder | sort_order | INT | ✅ 匹配 |
| remark | remark | remark | TEXT | ✅ 匹配 |

**枚举值验证:**
- `STRAT_TO_FUNC` → IndicatorLevel.STRAT_TO_FUNC ✅
- `FUNC_TO_COLLEGE` → IndicatorLevel.FUNC_TO_COLLEGE ✅
- `ACTIVE` → IndicatorStatus.ACTIVE ✅

### 5. sync-milestone.js → Milestone Entity

| 脚本字段 | Entity 字段 | 数据库列 | 类型 | 状态 |
|---------|------------|---------|------|------|
| indicator_id | indicator.indicatorId | indicator_id | BIGINT FK | ✅ 匹配 |
| milestone_name | milestoneName | milestone_name | VARCHAR(200) | ✅ 匹配 |
| milestone_desc | milestoneDesc | milestone_desc | TEXT | ✅ 匹配 |
| due_date | dueDate | due_date | DATE | ✅ 匹配 |
| weight_percent | weightPercent | weight_percent | NUMERIC(5,2) | ✅ 匹配 |
| status | status | status | milestone_status ENUM | ✅ 匹配 |
| sort_order | sortOrder | sort_order | INT | ✅ 匹配 |

**枚举值验证:**
- `NOT_STARTED` → MilestoneStatus.NOT_STARTED ✅
- `IN_PROGRESS` → MilestoneStatus.IN_PROGRESS ✅

## 同步顺序验证

同步脚本按以下顺序执行，确保外键依赖正确：

1. **org** - 无外键依赖
2. **cycle** - 无外键依赖
3. **task** - 依赖 org, cycle
4. **indicator** - 依赖 task, org
5. **milestone** - 依赖 indicator

✅ 同步顺序正确，满足外键约束要求

## 组织机构映射验证

脚本中定义的组织机构需要与数据库中的组织名称完全匹配：

### 职能部门 (FUNCTION_DEPT)
| 脚本定义 | 数据库存在 |
|---------|-----------|
| 党委办公室 \| 党委统战部 | 需同步创建 |
| 纪委办公室 \| 监察处 | 需同步创建 |
| 党委宣传部 \| 宣传策划部 | 需同步创建 |
| 党委组织部 \| 党委教师工作部 | 需同步创建 |
| 人力资源部 | 需同步创建 |
| 党委学工部 \| 学生工作处 | 需同步创建 |
| 党委保卫部 \| 保卫处 | 需同步创建 |
| 学校综合办公室 | 需同步创建 |
| 教务处 | ✅ 已存在 |
| 科技处 | 需同步创建 |
| 财务部 | 需同步创建 |
| 招生工作处 | 需同步创建 |
| 就业创业指导中心 | 需同步创建 |
| 实验室建设管理处 | 需同步创建 |
| 数字校园建设办公室 | 需同步创建 |
| 图书馆 \| 档案馆 | 需同步创建 |
| 后勤资产处 | 需同步创建 |
| 继续教育部 | 需同步创建 |
| 国际合作与交流处 | 需同步创建 |

### 二级学院 (COLLEGE)
| 脚本定义 | 数据库存在 |
|---------|-----------|
| 马克思主义学院 | 需同步创建 |
| 工学院 | 需同步创建 |
| 计算机学院 | ✅ 已存在 |
| 商学院 | ✅ 已存在 |
| 文理学院 | 需同步创建 |
| 艺术与科技学院 | 需同步创建 |
| 航空学院 | 需同步创建 |
| 国际教育学院 | 需同步创建 |

## 验证结论

### ✅ 通过项
1. 所有字段名称与 Entity 定义一致
2. 所有枚举值与后端 Enum 定义一致
3. 同步顺序正确，满足外键约束
4. 数据类型映射正确

### ⚠️ 注意事项
1. 同步脚本会自动创建缺失的组织机构
2. 重复执行同步脚本不会产生重复数据（幂等性）
3. 同步失败时会自动回滚事务

## 外键关联验证

### 验证脚本
位置: `strategic-task-management/database/verify-foreign-keys.sql`

### 外键依赖关系图

```
org (组织)
  ↑
  ├── strategic_task.org_id
  ├── strategic_task.created_by_org_id
  ├── indicator.owner_org_id
  ├── indicator.target_org_id
  ├── adhoc_task.creator_org_id
  └── app_user.org_id

assessment_cycle (考核周期)
  ↑
  ├── strategic_task.cycle_id
  └── adhoc_task.cycle_id

strategic_task (战略任务)
  ↑
  └── indicator.task_id

indicator (指标)
  ↑
  ├── indicator.parent_indicator_id (自引用)
  ├── milestone.indicator_id
  └── progress_report.indicator_id

milestone (里程碑)
  ↑
  ├── milestone.inherited_from (自引用)
  └── progress_report.milestone_id
```

### 同步脚本外键处理

1. **sync-org.js**: 无外键依赖，首先执行
2. **sync-cycle.js**: 无外键依赖，可与 org 并行
3. **sync-task.js**: 
   - 依赖 `org` (org_id, created_by_org_id)
   - 依赖 `assessment_cycle` (cycle_id)
4. **sync-indicator.js**:
   - 依赖 `strategic_task` (task_id)
   - 依赖 `org` (owner_org_id, target_org_id)
   - 自引用 `indicator` (parent_indicator_id)
5. **sync-milestone.js**:
   - 依赖 `indicator` (indicator_id)

### 外键验证方法

执行验证脚本:
```bash
psql -U $DB_USER -d $DB_NAME -f strategic-task-management/database/verify-foreign-keys.sql
```

### 预期结果
- 所有外键引用应指向有效记录
- 无效引用查询应返回空结果集
- 汇总统计中所有 invalid_* 列应为 0

## 建议
1. 首次执行同步前，确保数据库连接配置正确
2. 建议在测试环境先执行验证
3. 生产环境执行前备份数据库
4. 同步完成后执行 `verify-foreign-keys.sql` 验证外键有效性
