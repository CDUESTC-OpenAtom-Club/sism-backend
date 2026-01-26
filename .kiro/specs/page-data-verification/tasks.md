# Implementation Plan: Page Data Verification

## Overview

本实现计划将页面数据检查与修复功能分解为可执行的编码任务。实现采用渐进式方法，先创建核心验证工具，然后逐页检查和修复数据问题，最后添加测试覆盖。

## Tasks

- [x] 1. 创建数据验证基础设施
  - [x] 1.1 创建验证规则配置文件
    - 在 `src/config/validationRules.ts` 创建指标、里程碑、用户的验证规则
    - 定义必填字段、类型、范围、枚举值等规则
    - _Requirements: 2.4, 2.6, 5.2, 9.1, 9.2, 9.3_
  
  - [x] 1.2 创建 DataValidator Composable
    - 在 `src/composables/useDataValidator.ts` 实现数据验证组合式函数
    - 实现 validateIndicator、validateMilestone、validateUser 方法
    - 实现 safeGet 安全取值方法，处理 null/undefined
    - _Requirements: 2.4, 3.4, 9.4_
  
  - [x] 1.3 编写 DataValidator 属性测试
    - **Property 3: Data Completeness Validation**
    - **Property 4: Enum Value Validation**
    - **Property 5: Data Format Validation**
    - **Validates: Requirements 2.4, 2.6, 5.2, 9.1, 9.2, 9.3**

- [x] 2. 创建加载状态和错误处理组件
  - [x] 2.1 创建 LoadingState Composable
    - 在 `src/composables/useLoadingState.ts` 实现加载状态管理
    - 实现 startLoading、endLoading、setError、retry 方法
    - 支持超时检测和重试逻辑
    - _Requirements: 1.5, 10.1, 10.2_
  
  - [x] 2.2 创建 ErrorHandler Composable
    - 在 `src/composables/useErrorHandler.ts` 实现错误处理
    - 实现 handleApiError、showErrorMessage、getFriendlyMessage 方法
    - 集成 Element Plus 的 ElMessage 组件显示提示
    - _Requirements: 2.5, 10.3, 10.4_
  
  - [x] 2.3 编写错误处理单元测试
    - 测试各类错误的友好消息生成
    - 测试重试逻辑
    - _Requirements: 10.2, 10.3, 10.4_

- [x] 3. 增强 Strategic Store 数据加载
  - [x] 3.1 添加数据来源标记和加载状态
    - 在 `src/stores/strategic.ts` 添加 dataSource、loadingState、validationState
    - 实现 loadFromApi 方法从后端加载数据
    - 添加数据加载失败时的降级逻辑
    - _Requirements: 1.1, 7.1, 8.1_
  
  - [x] 3.2 实现数据验证和健康检查
    - 实现 validateCurrentData 方法验证 Store 中的数据
    - 实现 getDataHealth 方法返回数据健康状态
    - _Requirements: 8.4_
  
  - [x] 3.3 编写 Store 数据加载属性测试
    - **Property 1: Data Source Verification**
    - **Property 7: Fallback Mechanism Trigger**
    - **Validates: Requirements 1.1, 1.4, 7.4, 8.1**

- [x] 4. Checkpoint - 基础设施验证
  - 确保所有测试通过，ask the user if questions arise.

- [x] 5. 检查并修复 DashboardView 数据问题
  - [x] 5.1 审查 DashboardView 数据来源
    - 检查 dashboardData computed 是否正确从 Store 获取数据
    - 检查 departmentSummary 计算逻辑是否正确
    - 检查 alertDistribution 预警分布计算是否基于实际数据
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [x] 5.2 添加加载状态和空状态处理
    - 在 DashboardView 中集成 useLoadingState
    - 添加数据加载中的骨架屏显示
    - 添加数据为空时的空状态提示组件
    - _Requirements: 1.5, 1.6_
  
  - [x] 5.3 添加降级模式提示
    - 检测 Store 的 dataSource 是否为 fallback
    - 显示降级模式提示标识
    - _Requirements: 1.4, 10.5_
  
  - [x] 5.4 编写 Dashboard 数据计算属性测试
    - **Property 2: Filter Logic Correctness** (部门汇总计算)
    - **Validates: Requirements 1.2, 1.3**

- [x] 6. 检查并修复 IndicatorListView 数据问题
  - [x] 6.1 审查指标列表数据来源和过滤逻辑
    - 检查 indicators computed 是否正确从 strategicStore 获取
    - 检查年份过滤逻辑是否正确
    - 检查部门过滤逻辑（responsibleDept、ownerDept）是否正确
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [x] 6.2 验证里程碑数据完整性
    - 检查 milestones 数组字段是否完整
    - 添加里程碑数据验证，缺失字段时显示默认值
    - _Requirements: 2.4_
  
  - [x] 6.3 验证审批状态枚举值
    - 检查 progressApprovalStatus 是否为有效枚举值
    - 添加无效状态的容错处理
    - _Requirements: 2.6_
  
  - [x] 6.4 编写指标过滤属性测试
    - **Property 2: Filter Logic Correctness** (年份、部门过滤)
    - **Validates: Requirements 2.2, 2.3**

- [x] 7. 检查并修复 IndicatorDistributionView 数据问题
  - [x] 7.1 审查学院列表和子指标数据
    - 检查 colleges 是否从 orgStore 正确获取
    - 检查子指标的 parentIndicatorId 关联是否正确
    - _Requirements: 3.1, 3.2_
  
  - [x] 7.2 验证审批指标筛选和审计日志
    - 检查 approvalIndicators 是否正确筛选 pending 状态
    - 检查 statusAudit 审计日志字段完整性
    - _Requirements: 3.3, 3.4_
  
  - [x] 7.3 添加空状态处理
    - 学院无指标时显示"暂无指标"状态
    - _Requirements: 3.5_
  
  - [x] 7.4 编写父子指标关联属性测试
    - **Property 10: Parent-Child Indicator Association**
    - **Validates: Requirements 3.2**

- [x] 8. Checkpoint - 核心页面验证
  - 确保所有测试通过，ask the user if questions arise.

- [x] 9. 检查并修复 StrategicTaskView 数据问题
  - [x] 9.1 审查任务和指标数据来源
    - 检查 taskList 是否从 strategicStore.tasks 获取
    - 检查指标按部门筛选是否正确过滤 isStrategic=true
    - _Requirements: 4.1, 4.2_
  
  - [x] 9.2 验证权重和里程碑数据
    - 检查 weight 字段是否为有效数值
    - 检查里程碑编辑时数据完整性
    - _Requirements: 4.3, 4.4_
  
  - [x] 9.3 添加空状态处理
    - 任务无关联指标时显示空状态提示
    - _Requirements: 4.5_

- [x] 10. 检查并修复 ProfileView 和 MessageCenterView 数据问题
  - [x] 10.1 审查 ProfileView 用户数据
    - 检查 currentUser 是否从 authStore.user 获取
    - 验证 role 和 department 字段
    - 添加用户信息缺失时的默认值处理
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [x] 10.2 审查 MessageCenterView 消息数据
    - 检查预警消息是否基于 strategicStore 实际数据生成
    - 验证 getOverdueMilestones 和 getUpcomingMilestones 逻辑
    - 验证未读消息计数逻辑
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [x] 10.3 编写里程碑日期判断属性测试
    - **Property 9: Milestone Date Judgment**
    - **Validates: Requirements 6.2, 6.3**

- [x] 11. 验证 API 层数据转换
  - [x] 11.1 审查 VO 到前端类型转换
    - 检查 `src/api/strategic.ts` 中的 convertIndicatorVOToStrategicIndicator
    - 验证所有字段正确映射
    - 添加缺失字段的默认值处理
    - _Requirements: 7.3_
  
  - [x] 11.2 验证降级机制
    - 检查 fallback.ts 降级服务是否正确触发
    - 验证降级日志记录
    - _Requirements: 7.4, 7.5_
  
  - [x] 11.3 编写 VO 转换属性测试
    - **Property 8: VO to Frontend Type Conversion**
    - **Validates: Requirements 7.3**

- [x] 12. 创建 PageDataChecker 服务
  - [x] 12.1 实现页面数据检查服务
    - 在 `src/services/pageDataChecker.ts` 创建检查服务
    - 实现各页面的数据检查方法
    - 实现检查报告生成
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 6.1_
  
  - [x] 12.2 添加开发环境数据检查工具
    - 在开发环境下提供数据检查入口
    - 支持控制台输出检查报告
    - _Requirements: 8.4_

- [x] 13. 添加空值和格式处理
  - [x] 13.1 统一日期格式化
    - 检查所有日期显示是否使用统一格式
    - 创建日期格式化工具函数
    - _Requirements: 9.1_
  
  - [x] 13.2 统一空值处理
    - 检查所有字段的空值处理
    - 确保 null/undefined/空数组 显示默认值或占位符
    - _Requirements: 9.4, 9.5_
  
  - [x] 13.3 编写空值处理属性测试
    - **Property 6: Null Value Handling**
    - **Validates: Requirements 9.4, 9.5**

- [x] 14. Final Checkpoint - 完整验证
  - 确保所有测试通过，ask the user if questions arise.
  - 运行完整的数据检查报告
  - 验证所有页面数据显示正常

## Notes

- All tasks are required for comprehensive coverage
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- 实现语言：TypeScript (Vue 3 + Composition API)
- 测试框架：Vitest + fast-check
