# Implementation Plan

## Phase 1: 确立标准 - 提取前端数据字典

- [x] 1. 分析前端 Mock 数据结构






  - [x] 1.1 解析 `indicators2026.ts` 提取所有指标字段

    - 读取 `strategic-task-management/src/data/indicators/indicators2026.ts`
    - 提取 `StrategicIndicator` 对象的所有字段名、类型、示例值
    - _Requirements: 1.1, 1.2_

  - [x] 1.2 解析 TypeScript 类型定义

    - 读取 `strategic-task-management/src/types/index.ts`
    - 提取 `StrategicIndicator`, `Milestone`, `StatusAuditEntry` 接口定义
    - _Requirements: 1.1, 1.2_

  - [x] 1.3 生成前端数据字典文档

    - 输出字段名、类型、业务含义的结构化文档
    - _Requirements: 1.3, 1.4_

## Phase 2: 差距分析 - 对比前后端数据结构


- [x] 2. 分析后端数据结构





  - [x] 2.1 解析后端 Entity 类

    - 读取 `sism-backend/src/main/java/com/sism/entity/Indicator.java`
    - 读取 `sism-backend/src/main/java/com/sism/entity/Milestone.java`
    - 提取所有 Java 属性和数据库列映射
    - _Requirements: 2.1_

  - [x] 2.2 解析后端 VO 类

    - 读取 `sism-backend/src/main/java/com/sism/vo/IndicatorVO.java`
    - 读取 `sism-backend/src/main/java/com/sism/vo/MilestoneVO.java`
    - 提取 API 响应字段
    - _Requirements: 2.1_
  - [x] 2.3 生成差距分析报告


    - 对比前端字段与后端字段
    - 标记缺失字段 (❌)、类型不匹配 (⚠️)、已存在 (✅)
    - 输出《待补充字段列表》
    - _Requirements: 2.2, 2.3, 2.4_


- [x] 2.4 编写差距分析属性测试

  - **Property 3: 差距分类正确性**
  - **Validates: Requirements 2.1, 2.2, 2.3**

## Phase 3: 结构同步 - 更新数据库 Schema

- [x] 3. 更新数据库表结构





  - [x] 3.1 编写 ALTER TABLE SQL 脚本


    - 为 `indicator` 表添加缺失字段
    - 包含：`is_qualitative`, `type1`, `type2`, `can_withdraw`, `target_value`, `unit`, `responsible_person`, `status_audit`, `progress_approval_status`, `pending_progress`, `pending_remark`
    - _Requirements: 3.1, 3.2_
  - [x] 3.2 更新后端 Indicator Entity


    - 添加新字段的 Java 属性
    - 添加 JPA 注解映射
    - _Requirements: 3.4_
  - [x] 3.3 更新后端 IndicatorVO


    - 添加新字段到 VO 类
    - 确保字段名使用 camelCase
    - _Requirements: 5.1, 5.2_
  - [x] 3.4 更新 IndicatorService 映射逻辑


    - 在 Entity 到 VO 转换中包含新字段
    - _Requirements: 5.3_

- [x] 3.5 编写 Entity 字段覆盖属性测试


  - **Property 7: Entity 字段覆盖**
  - **Validates: Requirements 5.1**

- [x] 3.6 编写 VO 字段覆盖属性测试


  - **Property 8: VO 字段覆盖**
  - **Validates: Requirements 5.2**


- [x] 4. Checkpoint - 确保 Schema 变更正确




  - Ensure all tests pass, ask the user if questions arise.

## Phase 4: 内容对齐 - 补充数据库数据


- [x] 5. 补充指标种子数据





  - [x] 5.1 编写定量指标 INSERT 语句

    - 从 `indicators2026.ts` 提取定量指标数据
    - 确保至少 12 条定量指标
    - 使用 Mock 数据的精确值（不使用占位符）
    - _Requirements: 4.1, 4.2, 7.1_


  - [x] 5.2 编写定性指标 INSERT 语句
    - 从 `indicators2026.ts` 提取定性指标数据
    - 包含自定义里程碑配置
    - _Requirements: 4.1, 4.2, 7.2_
  - [x] 5.3 编写里程碑 INSERT 语句

    - 为每个指标创建对应的里程碑记录
    - 定量指标：12 个月度里程碑
    - 定性指标：自定义里程碑


    - _Requirements: 7.4_
  - [x] 5.4 编写审计日志种子数据
    - 从 Mock 数据的 `statusAudit` 字段提取
    - 插入到 `status_audit` JSON 字段
    - _Requirements: 4.1_






  - [x] 5.5 验证外键关系
    - 确保 `owner_org_id` 和 `target_org_id` 引用有效的组织
    - 确保 `parent_indicator_id` 引用有效的父指标
    - _Requirements: 4.3, 5.3_


- [x] 5.6 编写种子数据值保真属性测试
  - **Property 6: 种子数据值保真**
  - **Validates: Requirements 4.1, 4.2**

- [x] 5.7 编写里程碑数据完整性属性测试
  - **Property 13: 里程碑数据完整性**
  - **Validates: Requirements 7.4**


- [x] 6. Checkpoint - 确保数据插入正确
  - Ensure all tests pass, ask the user if questions arise.

## Phase 5: 接口放行 - 验证 API 响应

- [x] 7. 验证 API 响应格式

  - [x] 7.1 测试指标列表 API
    - 调用 `GET /api/indicators` 接口
    - 验证响应包含所有前端需要的字段
    - _Requirements: 5.3, 5.4_
  - [x] 7.2 测试指标详情 API
    - 调用 `GET /api/indicators/{id}` 接口
    - 验证里程碑、审计日志等嵌套数据
    - _Requirements: 7.4, 8.3_
  - [x] 7.3 测试指标过滤 API
    - 测试按类型过滤（定性/定量）
    - 测试按状态过滤
    - _Requirements: 7.3, 7.5_

- [x] 7.4 编写 API 响应字段匹配属性测试
  - **Property 9: API 响应字段匹配**
  - **Validates: Requirements 5.3**

- [x] 7.5 编写指标类型过滤正确性属性测试


  - **Property 12: 指标类型过滤正确性**
  - **Validates: Requirements 7.3, 7.5**

## Phase 6: 数据一致性验证


- [x] 8. 验证前后端数据一致性


  - [x] 8.1 对比 Mock 数据与 API 响应


    - 逐字段比对指标数据
    - 记录任何不一致
    - _Requirements: 6.2, 6.3_

  - [x] 8.2 验证数据完整性
    - 确认定量指标数量 >= 12
    - 确认定性指标有自定义里程碑
    - 确认发展性和基础性指标都存在
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 8.3 编写序列化往返一致性属性测试


  - **Property 10: 序列化往返一致性**
  - **Validates: Requirements 6.1**

- [x] 8.4 编写 Mock 与 API 数据一致性属性测试
  - **Property 11: Mock 与 API 数据一致性**
  - **Validates: Requirements 6.2, 6.3**

- [x] 9. Final Checkpoint - 确保所有测试通过
  - All data-alignment-sop property tests pass
  - Note: sensitive-info test failure is pre-existing and unrelated to this spec
