# 数据库废弃表混乱逻辑审查报告

**审查日期**: 2026-02-13
**审查人**: Claude AI Agent
**状态**: ⚠️ **发现严重问题**

---

## 📊 执行概要

### 审查范围
1. 数据库中废弃表的数据状态
2. 数据完整性检查(是否有引用废弃表的记录)
3. 字段命名冲突检查
4. 序列和索引检查
5. 后端代码审查(实体类和Repository)

### 审查结果
- ⚠️ **发现 4 个废弃表仍在使用**
- ⚠️ **发现 124 条废弃数据**
- ⚠️ **发现 703 条数据引用已废弃表**
- ⚠️ **发现 2 个遗留实体类未删除**

---

## 🔍 详细发现

### 1. 废弃表数据统计

| 表名 | 数据量 | 状态 | 问题 |
|------|--------|------|------|
| **app_user** | 0 条 | ✅ 已清空 | 实体类 AppUser.java 未删除 |
| **org_deprecated** | 27 条 | ⚠️ 有数据 | 旧组织数据,应已迁移至 sys_org |
| **sys_user_deprecated** | 57 条 | ⚠️ 有数据 | 旧用户数据,应已迁移至 sys_user |
| **task_deprecated** | 40 条 | ⚠️ 有数据 | 旧任务数据,应已迁移至 strategic_task |
| **总计** | **124 条** | ⚠️ 占用存储 | **应该清理** |

**问题**:
- ❌ **org_deprecated**, **sys_user_deprecated**, **task_deprecated** 三张表共 124 条废弃数据未清理
- ❌ 虽然已迁移到新表,但旧表数据未删除,占用存储空间
- ❌ 可能导致查询混乱(如果应用层错误地查询了旧表)

---

### 2. 数据完整性问题

#### ⚠️ indicator 表严重问题

**发现 703 条记录的 task_id 字段可能引用无效数据**

```sql
-- 检查SQL
SELECT COUNT(*)
FROM indicator i
LEFT JOIN strategic_task st ON st.task_id = i.task_id
WHERE i.task_id IS NOT NULL AND st.task_id IS NULL;
-- 结果: 703 条
```

**问题分析**:
- indicator 表有 711 条数据
- **703 条(约 99%)的 task_id 可能引用不存在的 strategic_task**
- 可能原因:
  1. 数据迁移时未正确更新 task_id 引用
  2. indicator 表原本引用 task_deprecated,迁移后未更新
  3. strategic_task 表只有 4 条数据,无法匹配 703 条指标

**影响范围**:
- 所有指标查询功能
- 任务进度报告
- 数据统计和报表

**修复建议**:
1. 立即检查这 703 条指标的 task_id 正确值
2. 如果 strategic_task 表确实只有 4 条任务,说明大部分指标没有关联任务
3. 考虑是否允许指标不关联任务,或修复数据

#### ✅ 其他表数据完整性通过

| 表 | 字段 | 状态 |
|------|------|------|
| indicator | owner_org_id | ✅ 无NULL值 |
| indicator | target_org_id | ✅ 无NULL值 |
| sys_user | org_id | ✅ 无NULL值 |

---

### 3. 后端代码审查

#### ⚠️ 遗留实体类

**1. AppUser.java** (应删除)

**位置**: `src/main/java/com/sism/entity/AppUser.java`

**问题**:
```java
@Entity
@Table(name = "app_user")  // ⚠️ 指向已废弃的 app_user 表
public class AppUser extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;  // ⚠️ 引用 Org 实体(而非 SysOrg)
}
```

**应该使用**: `SysUser.java` (指向 sys_user 表)

**影响**:
- 如果代码中有使用 AppUser 的地方,会查询错误的表
- app_user 表虽然数据已清空,但实体类未删除会导致混乱

---

**2. Org.java** (命名混乱)

**位置**: `src/main/java/com/sism/entity/Org.java`

**问题**:
```java
@Entity
@Table(name = "sys_org", schema = "public")  // ✅ 表名正确
public class Org extends BaseEntity {  // ⚠️ 类名应该是 SysOrg
```

**应该是**: `SysOrg.java`

**当前正确实体**: `SysOrg.java` 存在于 `com.sism.entity`

**影响**:
- 两个实体类 Org 和 SysOrg 都指向 sys_org 表
- 可能导致代码中引用混乱
- 应该统一使用 SysOrg

---

### 4. 数据库索引问题

#### ⚠️ 发现 8 个指向废弃表的索引

| 表名 | 索引名 | 类型 | 问题 |
|------|--------|------|------|
| org_deprecated | org_pkey1 | UNIQUE | 废弃表索引 |
| org_deprecated | uk_org_name | UNIQUE | 废弃表索引 |
| sys_user_deprecated | app_user_pkey | UNIQUE | 废弃表索引 |
| sys_user_deprecated | app_user_username_key | UNIQUE | 废弃表索引 |
| sys_user_deprecated | idx_user_org | INDEX | 废弃表索引 |
| sys_user_deprecated | idx_user_username | INDEX | 废弃表索引 |
| task_deprecated | idx_task_cycle | INDEX | 废弃表索引 |
| task_deprecated | strategic_task_pkey | UNIQUE | 废弃表索引 |

**问题**:
- 这些索引占用存储空间
- 如果废弃表有数据,索引会影响写入性能
- 建议删除这些表和索引

---

## 🚨 严重问题总结

### 🔴 高优先级(立即修复)

1. **indicator 表 703 条记录 task_id 引用可能无效**
   - 影响所有指标相关功能
   - 需要立即数据修复
   - 建议检查这 703 条指标的正确 task_id

2. **124 条废弃数据未清理**
   - org_deprecated: 27 条
   - sys_user_deprecated: 57 条
   - task_deprecated: 40 条
   - 占用存储,可能引起查询混乱

3. **遗留实体类未删除**
   - `AppUser.java` - 应删除,使用 `SysUser.java`
   - `Org.java` - 应删除,使用 `SysOrg.java`

### 🟡 中优先级(近期修复)

4. **8 个废弃表索引未清理**
   - 占用存储空间
   - 建议删除废弃表后自动删除

5. **数据完整性验证缺失**
   - 应用层缺少 ID 有效性检查
   - 建议添加 Service 层数据验证方法

---

## 📋 修复建议

### 立即执行

#### 1. 清理废弃表数据 ⚠️ 高优先级

```sql
-- 备份数据(可选)
CREATE TABLE org_deprecated_backup AS SELECT * FROM org_deprecated;
CREATE TABLE sys_user_deprecated_backup AS SELECT * FROM sys_user_deprecated;
CREATE TABLE task_deprecated_backup AS SELECT * FROM task_deprecated;

-- 清空废弃表
TRUNCATE TABLE org_deprecated;
TRUNCATE TABLE sys_user_deprecated;
TRUNCATE TABLE task_deprecated;

-- 验证
SELECT COUNT(*) FROM org_deprecated; -- 应该是 0
SELECT COUNT(*) FROM sys_user_deprecated; -- 应该是 0
SELECT COUNT(*) FROM task_deprecated; -- 应该是 0
```

#### 2. 删除遗留实体类 ⚠️ 高优先级

```bash
# 删除或重命名遗留实体类
cd src/main/java/com/sism/entity

# 方案1: 删除
rm AppUser.java
rm Org.java

# 方案2: 重命名为 .deprecated(推荐)
mv AppUser.java AppUser.java.deprecated
mv Org.java Org.java.deprecated
```

#### 3. 修复 indicator 表的 task_id 引用 ⚠️ 高优先级

```sql
-- 第一步:检查问题数据
SELECT
    i.id,
    i.task_id,
    i.indicator_desc
FROM indicator i
LEFT JOIN strategic_task st ON st.task_id = i.task_id
WHERE i.task_id IS NOT NULL
  AND st.task_id IS NULL
LIMIT 10;

-- 第二步:根据业务逻辑修复
-- 选项A: 如果这些指标不需要关联任务,设为 NULL
UPDATE indicator
SET task_id = NULL
WHERE task_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM strategic_task WHERE task_id = indicator.task_id);

-- 选项B: 如果应该关联任务,找出正确的 task_id 并更新
-- (需要业务人员确认正确的关联关系)
```

### 后续工作

#### 4. 添加 Service 层数据验证

```java
@Service
public class DataValidationService {

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private StrategicTaskRepository taskRepository;

    /**
     * 验证组织ID存在性
     */
    public boolean orgExists(Long orgId) {
        if (orgId == null) return false;
        return orgRepository.existsById(orgId);
    }

    /**
     * 验证用户ID存在性
     */
    public boolean userExists(Long userId) {
        if (userId == null) return false;
        return userRepository.existsById(userId);
    }

    /**
     * 验证任务ID存在性
     */
    public boolean taskExists(Long taskId) {
        if (taskId == null) return true; // 指标可以不关联任务
        return taskRepository.existsById(taskId);
    }
}
```

#### 5. 创建数据清理脚本

```javascript
// scripts/cleanup-deprecated-data.js
// 定时任务:清理超过30天的废弃表数据

const cleanupTables = [
  'org_deprecated',
  'sys_user_deprecated',
  'task_deprecated'
];

for (const table of cleanupTables) {
  const sql = `
    DELETE FROM ${table}
    WHERE created_at < NOW() - INTERVAL '30 days'
  `;
  await client.query(sql);
}
```

---

## ✅ 验证清单

修复后,请验证以下项目:

### 数据库层面
- [ ] org_deprecated 表数据已清空
- [ ] sys_user_deprecated 表数据已清空
- [ ] task_deprecated 表数据已清空
- [ ] indicator 表的 703 条无效 task_id 已修复
- [ ] 废弃表的索引已删除

### 代码层面
- [ ] AppUser.java 已删除或重命名
- [ ] Org.java 已删除或重命名
- [ ] 全部代码使用 SysUser 和 SysOrg
- [ ] Service 层添加数据验证方法
- [ ] 单元测试覆盖数据验证场景

### 运行时验证
- [ ] 应用启动无错误(无找不到实体类)
- [ ] 指标查询功能正常
- [ ] 用户登录功能正常
- [ ] 数据统计报表正常

---

## 📈 影响评估

### 如果不修复

#### 立即影响
1. **indicator 表数据错误** - 703 条指标(99%)的 task_id 引用无效
   - 无法正确查询指标关联的任务
   - 任务进度报告数据不准确
   - 统计报表数据错误

2. **存储空间浪费** - 124 条废弃数据
   - 占用约 1-2MB 存储空间
   - 如果有索引,占用更多空间

3. **代码维护混乱** - 遗留实体类
   - 开发者可能误用 AppUser 而非 SysUser
   - 代码审查困难

#### 长期影响
1. **数据迁移风险** - 废弃表数据未清理
   - 后续数据迁移可能包含旧数据
   - 数据一致性难以保证

2. **扩展性受限** - 废弃表和索引
   - 分布式部署时需要额外处理
   - 数据库备份恢复时间增加

---

## 🎯 总结

### 发现的问题
1. ✅ **数据库检查完成** - 发现 4 个废弃表,124 条数据
2. ⚠️ **数据完整性问题** - indicator 表 703 条记录 task_id 引用无效
3. ⚠️ **遗留实体类** - AppUser.java 和 Org.java 未删除
4. ⚠️ **索引未清理** - 8 个废弃表索引占用空间

### 修复优先级
- 🔴 **高优先级**(立即): 修复 indicator.task_id 引用,清理废弃表数据,删除遗留实体类
- 🟡 **中优先级**(本周): 清理废弃表索引,添加数据验证
- 🟢 **低优先级**(本月): 添加定时清理任务,完善监控

### 下一步行动
1. 与业务确认 indicator 表的 703 条数据正确处理方式
2. 执行废弃表数据清理(需先备份)
3. 删除或重命名遗留实体类
4. 添加 Service 层数据验证逻辑
5. 验证所有修复

---

**审查完成时间**: 2026-02-13
**报告版本**: v1.0
**下次审查**: 修复完成后验证
