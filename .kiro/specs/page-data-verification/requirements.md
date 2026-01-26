# Requirements Document

## Introduction

本需求文档定义了SISM战略指标管理系统各页面数据显示检查与修复的功能需求。目标是确保所有页面正确调用数据库数据，识别并修复数据缺失、格式异常、API调用失败等问题，提升系统数据展示的完整性和准确性。

## Glossary

- **Page_Data_Checker**: 页面数据检查器，负责验证页面数据是否正确从API获取
- **API_Layer**: API请求层，包含Axios实例、拦截器和降级机制
- **Pinia_Store**: 状态管理层，负责存储和管理应用数据
- **Fallback_Service**: 降级服务，在API失败时提供模拟数据
- **Data_Validator**: 数据验证器，检查数据格式和完整性
- **Loading_State**: 加载状态，显示数据获取进度
- **Error_Handler**: 错误处理器，处理API调用失败情况

## Requirements

### Requirement 1: 数据看板页面数据检查

**User Story:** As a 系统管理员, I want to 确保数据看板页面正确显示数据库数据, so that 我可以准确了解战略任务执行情况。

#### Acceptance Criteria

1. WHEN DashboardView页面加载时, THE Page_Data_Checker SHALL 验证总得分、基础性得分、发展性得分数据来源于API而非硬编码
2. WHEN 部门完成情况图表渲染时, THE Page_Data_Checker SHALL 验证departmentSummary数据从strategicStore正确获取
3. WHEN 预警分布图表显示时, THE Page_Data_Checker SHALL 验证alertDistribution数据基于实际指标进度计算
4. IF API调用失败, THEN THE Fallback_Service SHALL 提供降级数据并显示降级提示
5. WHEN 数据加载中, THE Loading_State SHALL 显示加载指示器
6. WHEN 数据为空, THE Page_Data_Checker SHALL 显示空状态提示而非空白页面

### Requirement 2: 指标列表页面数据检查

**User Story:** As a 职能部门用户, I want to 确保指标列表页面正确显示我负责的指标数据, so that 我可以准确管理和填报指标进度。

#### Acceptance Criteria

1. WHEN IndicatorListView页面加载时, THE Page_Data_Checker SHALL 验证indicators数据从strategicStore.indicators获取
2. WHEN 指标按年份筛选时, THE Data_Validator SHALL 验证year字段正确过滤数据
3. WHEN 指标按部门筛选时, THE Data_Validator SHALL 验证responsibleDept和ownerDept字段正确匹配
4. WHEN 里程碑数据显示时, THE Data_Validator SHALL 验证milestones数组包含完整的id、name、targetProgress、deadline、status字段
5. IF 指标数据缺少必要字段, THEN THE Error_Handler SHALL 记录错误并显示友好提示
6. WHEN 审批状态显示时, THE Data_Validator SHALL 验证progressApprovalStatus字段值为有效枚举值

### Requirement 3: 指标下发与审批页面数据检查

**User Story:** As a 职能部门用户, I want to 确保指标下发页面正确显示待下发和待审批的指标, so that 我可以准确完成指标分解和审批工作。

#### Acceptance Criteria

1. WHEN IndicatorDistributionView页面加载时, THE Page_Data_Checker SHALL 验证学院列表从orgStore正确获取
2. WHEN 子指标显示时, THE Data_Validator SHALL 验证parentIndicatorId正确关联父指标
3. WHEN 审批指标列表显示时, THE Data_Validator SHALL 验证progressApprovalStatus为pending的指标正确筛选
4. WHEN statusAudit审计日志显示时, THE Data_Validator SHALL 验证每条记录包含operator、action、timestamp字段
5. IF 学院没有下发指标, THEN THE Page_Data_Checker SHALL 显示"暂无指标"状态而非空白

### Requirement 4: 战略任务管理页面数据检查

**User Story:** As a 战略发展部用户, I want to 确保战略任务页面正确显示所有战略任务和指标, so that 我可以全面管理战略任务执行。

#### Acceptance Criteria

1. WHEN StrategicTaskView页面加载时, THE Page_Data_Checker SHALL 验证tasks数据从strategicStore.tasks获取
2. WHEN 指标按部门筛选时, THE Data_Validator SHALL 验证selectedDepartment正确过滤isStrategic=true的指标
3. WHEN 指标权重显示时, THE Data_Validator SHALL 验证weight字段为有效数值且总和符合业务规则
4. WHEN 里程碑编辑时, THE Data_Validator SHALL 验证里程碑数据包含完整的targetProgress和deadline
5. IF 任务没有关联指标, THEN THE Page_Data_Checker SHALL 显示空状态提示

### Requirement 5: 个人信息页面数据检查

**User Story:** As a 系统用户, I want to 确保个人信息页面正确显示我的账户信息, so that 我可以管理个人资料。

#### Acceptance Criteria

1. WHEN ProfileView页面加载时, THE Page_Data_Checker SHALL 验证currentUser数据从authStore.user获取
2. WHEN 用户角色显示时, THE Data_Validator SHALL 验证role字段为有效角色枚举值
3. WHEN 部门信息显示时, THE Data_Validator SHALL 验证department字段非空且与组织机构数据匹配
4. IF 用户信息缺失, THEN THE Error_Handler SHALL 显示默认值而非空白

### Requirement 6: 消息中心页面数据检查

**User Story:** As a 系统用户, I want to 确保消息中心正确显示预警和通知消息, so that 我可以及时了解系统动态。

#### Acceptance Criteria

1. WHEN MessageCenterView页面加载时, THE Page_Data_Checker SHALL 验证预警消息基于strategicStore实际数据生成
2. WHEN 逾期里程碑预警显示时, THE Data_Validator SHALL 验证getOverdueMilestones返回正确的逾期数据
3. WHEN 即将到期里程碑预警显示时, THE Data_Validator SHALL 验证getUpcomingMilestones返回正确的预警数据
4. WHEN 未读消息计数显示时, THE Data_Validator SHALL 验证unreadCount正确统计各类型未读消息

### Requirement 7: API层数据获取验证

**User Story:** As a 开发者, I want to 确保API层正确从后端获取数据, so that 前端显示的数据与数据库一致。

#### Acceptance Criteria

1. WHEN strategicApi.getAllIndicators调用时, THE API_Layer SHALL 返回数据库中所有指标数据
2. WHEN strategicApi.getIndicatorsByYear调用时, THE API_Layer SHALL 正确按年份过滤指标
3. WHEN API返回数据时, THE Data_Validator SHALL 验证VO到前端类型的转换正确完成
4. IF API返回错误, THEN THE Error_Handler SHALL 记录错误详情并触发降级机制
5. WHEN 使用降级数据时, THE Fallback_Service SHALL 在控制台记录降级原因

### Requirement 8: Store层数据管理验证

**User Story:** As a 开发者, I want to 确保Pinia Store正确管理和提供数据, so that 组件可以获取准确的状态数据。

#### Acceptance Criteria

1. WHEN strategicStore初始化时, THE Pinia_Store SHALL 从API获取数据而非使用硬编码数据
2. WHEN dashboardStore计算visibleIndicators时, THE Data_Validator SHALL 验证按角色和年份正确过滤
3. WHEN orgStore提供部门列表时, THE Data_Validator SHALL 验证数据与数据库组织机构表一致
4. IF Store数据与API数据不一致, THEN THE Error_Handler SHALL 记录数据同步错误

### Requirement 9: 数据格式和空值处理

**User Story:** As a 系统用户, I want to 确保页面正确处理各种数据格式和空值情况, so that 不会看到格式错误或空白内容。

#### Acceptance Criteria

1. WHEN 日期字段显示时, THE Data_Validator SHALL 验证日期格式为统一的中文格式
2. WHEN 进度百分比显示时, THE Data_Validator SHALL 验证progress值在0-100范围内
3. WHEN 权重显示时, THE Data_Validator SHALL 验证weight为有效数值
4. IF 字段值为null或undefined, THEN THE Page_Data_Checker SHALL 显示默认值或占位符
5. WHEN 数组字段为空时, THE Page_Data_Checker SHALL 显示空状态提示而非渲染错误

### Requirement 10: 加载状态和错误提示

**User Story:** As a 系统用户, I want to 在数据加载时看到加载状态，在出错时看到友好提示, so that 我了解系统当前状态。

#### Acceptance Criteria

1. WHEN 页面数据加载中, THE Loading_State SHALL 显示骨架屏或加载动画
2. WHEN API调用超时, THE Error_Handler SHALL 显示超时提示并提供重试选项
3. WHEN 网络错误发生, THE Error_Handler SHALL 显示网络错误提示
4. WHEN 数据加载失败, THE Error_Handler SHALL 显示错误详情和建议操作
5. WHEN 使用降级数据时, THE Page_Data_Checker SHALL 显示降级模式提示标识
