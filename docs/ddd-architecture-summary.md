# SISM DDD架构重构概要

## 概述

本文档描述SISM项目向DDD（领域驱动设计）架构演进的计划。

## 关键原则

1. **API兼容性**：保持现有Controller和API接口不变
2. **渐进式重构**：逐步将业务逻辑从Service迁移到领域模型
3. **数据库冻结**：不修改现有数据库Schema
4. **API文档优先**：严格按照API接口文档进行开发

## Bounded Contexts（限界上下文）

### 1. Strategy Context（战略上下文）

**职责**：战略规划和指标管理

**核心聚合根**：
- StrategicIndicator（指标）
- StrategicTask（战略任务）
- AssessmentCycle（评估周期）

**领域服务位置**：
- `com.sism.strategy.domain.*`
- `com.sism.strategy.application.*`

### 2. Execution Context（执行上下文）

**职责**：计划制定和执行跟踪

**核心聚合根**：
- Plan（计划）
- Milestone（里程碑）
- PlanReport（计划报告）

**领域服务位置**：
- `com.sism.execution.domain.*`
- `com.sism.execution.application.*`

### 3. Analytics Context（分析上下文）

**职责**：数据统计和导出

**核心模型**：
- Dashboard（仪表盘）
- Statistics（统计）
- DataExporter（导出）

**领域服务位置**：
- `com.sism.analytics.domain.*`
- `com.sism.analytics.application.*`

## 重构路线图

### 阶段1：战略领域建模
- 创建Strategy Context的基础包结构
- 实现StrategicIndicator领域模型
- 实现StrategicTask领域模型
- 创建领域事件机制
- 创建应用服务层

### 阶段2：执行领域建模
- 创建Execution Context的基础包结构
- 实现Plan领域模型
- 实现Milestone领域模型
- 实现PlanReport领域模型
- 创建领域事件机制
- 创建应用服务层

### 阶段3：分析领域建模
- 创建Analytics Context的基础包结构
- 实现Dashboard统计模型
- 实现Statistics分析模型
- 实现DataExporter导出功能

## 当前状态

- [ ] 阶段1：进行中
- [ ] 阶段2：待开始
- [ ] 阶段3：待开始

## 注意事项

1. 所有新的领域模型都应该使用现有实体作为基础，避免数据库Schema变更
2. 现有Controller保持不变，内部可以逐步调用新的应用服务
3. 现有Repository保持不变，新的领域仓储可以适配现有Repository
