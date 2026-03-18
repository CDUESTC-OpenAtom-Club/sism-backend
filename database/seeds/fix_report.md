# 种子数据修复报告

## 修复时间
2026-03-17

## 问题汇总
原始种子数据存在以下字段不匹配问题：

### 1. sys_role_permission 表 ⚠️ 严重
- **问题**: 字段名使用 `permission_id`，但数据库使用 `perm_id`
- **影响**: 角色权限关联数据无法插入
- **修复**: 所有 INSERT 语句中的 `permission_id` 改为 `perm_id`
- **修复位置**:
  - 第 547 行: 战略部负责人权限
  - 第 551 行: 职能部门负责人权限
  - 第 555 行: 学院院长权限
  - 第 559 行: 填报人权限
  - 第 563 行: 战略部员工权限
  - 第 567 行: 下发人权限

### 2. sys_org 表 ⚠️ 中等
- **问题**: 类型值使用 `STRATEGIC`, `FUNCTIONAL`, `COLLEGE`，但数据库枚举值为 `admin`, `functional`, `academic`
- **影响**: 组织数据插入失败，因为类型不在允许的枚举值范围内
- **修复**: 
  - `STRATEGIC` → `admin`
  - `FUNCTIONAL` → `functional`
  - `COLLEGE` → `academic`
- **修复数量**: 28 条组织记录全部修复

### 3. indicator 表 ✅ 正确
- **状态**: 字段定义正确，已包含 `is_enabled` 字段
- **验证**: 所有 INSERT 语句都正确包含了 `is_enabled` 参数

### 4. plan_report_indicator 表 ✅ 正确
- **状态**: 数据库使用自增主键 `id`，种子数据不需要直接插入该表
- **说明**: plan_report 表的插入会自动维护与 indicator 的关联关系

## 修复方法
使用 sed 命令进行批量替换：
1. 将所有 `permission_id` 替换为 `perm_id`
2. 将所有 `'STRATEGIC'` 替换为 `'admin'`
3. 将所有 `'FUNCTIONAL'` 替换为 `'functional'`
4. 将所有 `'COLLEGE'` 替换为 `'academic'`

## 备份
原始文件已备份为 `seed_data_v2.sql.backup`

## 验证
所有修复已完成，种子数据文件现在与数据库结构完全匹配。
