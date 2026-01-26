# Requirements Document

## Introduction

本规范定义了将战略指标管理系统（SISM）部署到生产服务器的完整需求。目标是确保前后端系统能够稳定对接、数据结构完全对齐、通过全面的测试验证，并最终成功部署到服务器上运行。

## Glossary

- **SISM (Strategic Indicator Management System)**: 战略指标管理系统
- **Frontend**: Vue 3 + TypeScript 前端应用，位于 `strategic-task-management/`
- **Backend**: Spring Boot 3.2 后端应用，位于 `sism-backend/`
- **API Contract**: 前后端接口约定，包括请求/响应格式、字段命名、数据类型
- **DTO (Data Transfer Object)**: 后端数据传输对象，定义 API 请求/响应结构
- **VO (View Object)**: 后端视图对象，用于 API 响应
- **Entity**: 后端数据库实体类，映射数据库表结构
- **Pinia Store**: 前端状态管理，存储应用数据
- **Integration Test**: 集成测试，验证前后端协同工作
- **E2E Test**: 端到端测试，模拟用户完整操作流程

## Requirements

### Requirement 1: 前后端 API 接口对齐

**User Story:** As a 开发者, I want to 确保前端 API 调用与后端接口完全匹配, so that 数据能够正确传输和解析。

#### Acceptance Criteria

1. WHEN 前端调用任意 API 端点 THEN THE System SHALL 使用与后端 Controller 定义一致的 URL 路径
2. WHEN 前端发送请求体 THEN THE System SHALL 使用与后端 DTO 字段名称和类型完全匹配的 JSON 结构
3. WHEN 后端返回响应 THEN THE System SHALL 使用前端 TypeScript 类型定义能够正确解析的 JSON 结构
4. WHEN 存在字段命名差异（如 camelCase vs snake_case） THEN THE System SHALL 统一为一种命名规范
5. WHEN API 文档更新 THEN THE System SHALL 同步更新前端类型定义和后端 DTO

### Requirement 2: 数据库 Schema 与实体对齐

**User Story:** As a 系统管理员, I want to 确保数据库表结构与后端实体类完全一致, so that JPA 能够正确映射数据。

#### Acceptance Criteria

1. WHEN 检查数据库表 THEN THE System SHALL 包含所有后端 Entity 类定义的字段
2. WHEN 检查外键关系 THEN THE System SHALL 与 Entity 中的 @ManyToOne/@OneToMany 注解一致
3. WHEN 检查字段类型 THEN THE System SHALL 与 Entity 中的 Java 类型兼容（如 VARCHAR 对应 String）
4. WHEN 检查枚举字段 THEN THE System SHALL 使用与后端 Enum 定义一致的值
5. WHEN 执行 schema 验证脚本 THEN THE System SHALL 输出所有不一致项的详细报告

### Requirement 3: 前端类型与后端 VO/DTO 对齐

**User Story:** As a 前端开发者, I want to 确保 TypeScript 类型定义与后端数据结构一致, so that 类型检查能够发现潜在问题。

#### Acceptance Criteria

1. WHEN 检查前端 `src/types/index.ts` THEN THE System SHALL 包含所有后端 VO 对应的 TypeScript 接口
2. WHEN 比较字段定义 THEN THE System SHALL 确认字段名称、类型、可选性完全匹配
3. WHEN 后端新增或修改 VO THEN THE System SHALL 同步更新前端类型定义
4. WHEN 执行 `npm run type-check` THEN THE System SHALL 无类型错误

### Requirement 4: 后端单元测试覆盖

**User Story:** As a 后端开发者, I want to 确保核心业务逻辑有充分的测试覆盖, so that 代码变更不会引入回归问题。

#### Acceptance Criteria

1. WHEN 执行 `mvn test` THEN THE System SHALL 所有测试用例通过
2. WHEN 检查 Service 层 THEN THE System SHALL 每个公开方法有对应的单元测试
3. WHEN 检查 Controller 层 THEN THE System SHALL 每个端点有对应的集成测试
4. WHEN 检查属性测试 THEN THE System SHALL 核心业务规则有 jqwik 属性测试覆盖
5. WHEN 测试失败 THEN THE System SHALL 输出清晰的失败原因和堆栈信息

### Requirement 5: 前端单元测试覆盖

**User Story:** As a 前端开发者, I want to 确保前端组件和工具函数有测试覆盖, so that UI 变更不会破坏现有功能。

#### Acceptance Criteria

1. WHEN 执行 `npm run test` THEN THE System SHALL 所有测试用例通过
2. WHEN 检查 Pinia Store THEN THE System SHALL 核心 action 有对应的单元测试
3. WHEN 检查工具函数 THEN THE System SHALL 公共 utils 有对应的单元测试
4. WHEN 检查属性测试 THEN THE System SHALL 数据转换逻辑有 fast-check 属性测试覆盖
5. WHEN 测试失败 THEN THE System SHALL 输出清晰的失败原因

### Requirement 6: 前后端集成测试

**User Story:** As a QA 工程师, I want to 验证前后端协同工作正常, so that 用户能够完成完整的业务流程。

#### Acceptance Criteria

1. WHEN 前端发起登录请求 THEN THE System SHALL 成功获取 JWT token 并存储
2. WHEN 前端请求指标列表 THEN THE System SHALL 正确显示后端返回的数据
3. WHEN 前端提交里程碑更新 THEN THE System SHALL 成功保存到数据库并返回更新后的数据
4. WHEN 前端请求审批流程 THEN THE System SHALL 正确执行审批状态流转
5. WHEN 后端返回错误 THEN THE System SHALL 前端正确显示错误提示

### Requirement 7: 生产环境配置

**User Story:** As a 运维工程师, I want to 准备好生产环境配置, so that 应用能够在服务器上稳定运行。

#### Acceptance Criteria

1. WHEN 配置后端生产环境 THEN THE System SHALL 使用 `application-prod.yml` 中的数据库连接
2. WHEN 配置前端生产构建 THEN THE System SHALL 使用正确的 API 基础 URL
3. WHEN 配置 CORS THEN THE System SHALL 允许生产域名的跨域请求
4. WHEN 配置日志 THEN THE System SHALL 输出到文件并设置合适的日志级别
5. WHEN 配置安全 THEN THE System SHALL 启用 HTTPS 并配置安全头

### Requirement 8: 部署脚本和文档

**User Story:** As a 运维工程师, I want to 获取完整的部署指南, so that 能够顺利完成服务器部署。

#### Acceptance Criteria

1. WHEN 查看部署文档 THEN THE System SHALL 提供服务器环境要求（JDK 17、Node.js 18+、PostgreSQL 15+）
2. WHEN 查看部署文档 THEN THE System SHALL 提供后端打包命令（mvn package）
3. WHEN 查看部署文档 THEN THE System SHALL 提供前端构建命令（npm run build）
4. WHEN 查看部署文档 THEN THE System SHALL 提供 Nginx 配置示例
5. WHEN 查看部署文档 THEN THE System SHALL 提供数据库初始化步骤
6. WHEN 查看部署文档 THEN THE System SHALL 提供服务启动和监控命令

### Requirement 9: 数据同步脚本验证

**User Story:** As a 系统管理员, I want to 确保数据同步脚本能够正确执行, so that 初始数据能够正确导入。

#### Acceptance Criteria

1. WHEN 执行 `npm run sync-db` THEN THE System SHALL 按顺序完成所有数据同步
2. WHEN 同步完成 THEN THE System SHALL 数据库中的记录数与前端数据一致
3. WHEN 同步完成 THEN THE System SHALL 所有外键关联有效
4. WHEN 重复执行同步 THEN THE System SHALL 不产生重复数据
5. WHEN 同步出错 THEN THE System SHALL 输出详细错误信息并回滚

### Requirement 10: 健康检查和监控

**User Story:** As a 运维工程师, I want to 监控应用运行状态, so that 能够及时发现和处理问题。

#### Acceptance Criteria

1. WHEN 访问后端健康检查端点 THEN THE System SHALL 返回应用状态和数据库连接状态
2. WHEN 应用启动完成 THEN THE System SHALL 记录启动日志和配置信息
3. WHEN 发生异常 THEN THE System SHALL 记录详细错误日志
4. WHEN 配置监控 THEN THE System SHALL 提供 Prometheus 指标端点（可选）

### Requirement 11: 性能测试

**User Story:** As a QA 工程师, I want to 验证系统在高负载下的性能表现, so that 确保生产环境能够承受预期的用户访问量。

#### Acceptance Criteria

1. WHEN 执行压力测试 THEN THE System SHALL 支持至少 100 个并发用户同时访问
2. WHEN 查询指标列表 THEN THE System SHALL 在 500ms 内返回响应
3. WHEN 提交数据更新 THEN THE System SHALL 在 1000ms 内完成处理
4. WHEN 系统负载达到 80% THEN THE System SHALL 保持稳定运行不崩溃
5. WHEN 执行性能测试 THEN THE System SHALL 生成包含响应时间、吞吐量、错误率的测试报告

### Requirement 12: 安全审计

**User Story:** As a 安全工程师, I want to 确保系统没有安全漏洞, so that 保护用户数据和系统安全。

#### Acceptance Criteria

1. WHEN 执行漏洞扫描 THEN THE System SHALL 无高危和中危安全漏洞
2. WHEN 检查依赖包 THEN THE System SHALL 无已知安全漏洞的依赖版本
3. WHEN 用户访问资源 THEN THE System SHALL 验证用户具有相应的角色权限
4. WHEN 用户尝试越权访问 THEN THE System SHALL 返回 403 Forbidden 并记录审计日志
5. WHEN 检查 API 端点 THEN THE System SHALL 所有敏感接口均需要认证
6. WHEN 检查数据传输 THEN THE System SHALL 敏感数据使用 HTTPS 加密传输
7. WHEN 检查密码存储 THEN THE System SHALL 使用 BCrypt 或同等强度算法加密存储

### Requirement 13: 数据库备份恢复

**User Story:** As a 运维工程师, I want to 建立数据库备份和恢复机制, so that 在数据丢失时能够快速恢复。

#### Acceptance Criteria

1. WHEN 配置自动备份 THEN THE System SHALL 每日凌晨执行全量数据库备份
2. WHEN 备份完成 THEN THE System SHALL 将备份文件存储到独立的备份服务器或云存储
3. WHEN 备份文件超过保留期限 THEN THE System SHALL 自动清理 30 天前的备份文件
4. WHEN 执行恢复操作 THEN THE System SHALL 能够从备份文件恢复到指定时间点
5. WHEN 备份或恢复失败 THEN THE System SHALL 发送告警通知给运维人员
6. WHEN 查看备份状态 THEN THE System SHALL 提供备份历史记录和文件大小信息

### Requirement 14: CI/CD 流水线

**User Story:** As a 开发者, I want to 建立自动化构建部署流水线, so that 代码变更能够快速安全地部署到生产环境。

#### Acceptance Criteria

1. WHEN 代码推送到 main 分支 THEN THE System SHALL 自动触发构建流水线
2. WHEN 构建流水线执行 THEN THE System SHALL 依次执行代码检查、单元测试、构建打包
3. WHEN 任一步骤失败 THEN THE System SHALL 停止流水线并通知开发者
4. WHEN 构建成功 THEN THE System SHALL 生成可部署的制品（JAR 包、前端静态文件）
5. WHEN 部署到测试环境 THEN THE System SHALL 自动执行冒烟测试验证部署成功
6. WHEN 部署到生产环境 THEN THE System SHALL 需要人工审批确认
7. WHEN 部署失败 THEN THE System SHALL 支持一键回滚到上一个稳定版本

### Requirement 15: 日志分析

**User Story:** As a 运维工程师, I want to 集中收集和分析系统日志, so that 能够快速定位和排查问题。

#### Acceptance Criteria

1. WHEN 应用运行 THEN THE System SHALL 将日志输出到统一的日志文件目录
2. WHEN 配置日志收集 THEN THE System SHALL 支持将日志发送到 ELK 或类似日志平台
3. WHEN 查询日志 THEN THE System SHALL 支持按时间范围、日志级别、关键字搜索
4. WHEN 发生错误 THEN THE System SHALL 日志包含完整的堆栈信息和请求上下文
5. WHEN 日志文件过大 THEN THE System SHALL 自动按日期或大小进行日志轮转
6. WHEN 检测到异常模式 THEN THE System SHALL 支持配置告警规则（如错误率突增）

### Requirement 11: 性能测试

**User Story:** As a QA 工程师, I want to 验证系统在高负载下的性能表现, so that 确保生产环境能够承受预期的用户访问量。

#### Acceptance Criteria

1. WHEN 执行 API 响应时间测试 THEN THE System SHALL 确保 95% 的请求在 500ms 内响应
2. WHEN 执行并发用户测试 THEN THE System SHALL 支持至少 50 个并发用户同时操作
3. WHEN 执行数据库查询测试 THEN THE System SHALL 确保复杂查询在 1 秒内返回结果
4. WHEN 执行前端页面加载测试 THEN THE System SHALL 确保首屏加载时间小于 3 秒
5. WHEN 发现性能瓶颈 THEN THE System SHALL 输出详细的性能分析报告

### Requirement 12: 安全审计

**User Story:** As a 安全工程师, I want to 验证系统的安全性, so that 保护用户数据和系统免受攻击。

#### Acceptance Criteria

1. WHEN 执行 SQL 注入测试 THEN THE System SHALL 所有输入参数经过参数化处理
2. WHEN 执行 XSS 测试 THEN THE System SHALL 所有用户输入经过转义处理
3. WHEN 检查认证机制 THEN THE System SHALL JWT token 有合理的过期时间和刷新机制
4. WHEN 检查权限控制 THEN THE System SHALL 每个 API 端点有正确的角色权限验证
5. WHEN 检查敏感数据 THEN THE System SHALL 密码使用 BCrypt 加密存储
6. WHEN 检查 HTTPS 配置 THEN THE System SHALL 生产环境强制使用 HTTPS
7. WHEN 执行依赖漏洞扫描 THEN THE System SHALL 无高危漏洞（使用 npm audit / OWASP Dependency Check）

### Requirement 13: 数据库备份恢复

**User Story:** As a 运维工程师, I want to 建立数据库备份和恢复机制, so that 在数据丢失时能够快速恢复。

#### Acceptance Criteria

1. WHEN 配置自动备份 THEN THE System SHALL 每日凌晨执行全量备份
2. WHEN 执行备份 THEN THE System SHALL 将备份文件存储到独立的存储位置
3. WHEN 备份完成 THEN THE System SHALL 保留最近 7 天的备份文件
4. WHEN 执行恢复测试 THEN THE System SHALL 能够从备份文件完整恢复数据库
5. WHEN 备份失败 THEN THE System SHALL 发送告警通知给运维人员

### Requirement 14: CI/CD 流水线

**User Story:** As a 开发者, I want to 建立自动化构建部署流程, so that 代码变更能够快速安全地部署到生产环境。

#### Acceptance Criteria

1. WHEN 代码推送到 main 分支 THEN THE System SHALL 自动触发构建流程
2. WHEN 执行构建 THEN THE System SHALL 运行所有单元测试和集成测试
3. WHEN 测试通过 THEN THE System SHALL 自动构建前端和后端产物
4. WHEN 构建成功 THEN THE System SHALL 将产物部署到测试环境
5. WHEN 测试环境验证通过 THEN THE System SHALL 支持一键部署到生产环境
6. WHEN 部署失败 THEN THE System SHALL 自动回滚到上一个稳定版本
7. WHEN 流水线执行 THEN THE System SHALL 记录每次构建和部署的详细日志

### Requirement 15: 日志分析

**User Story:** As a 运维工程师, I want to 集中收集和分析系统日志, so that 能够快速定位和排查问题。

#### Acceptance Criteria

1. WHEN 应用运行 THEN THE System SHALL 将日志输出到统一的日志文件
2. WHEN 记录日志 THEN THE System SHALL 包含时间戳、日志级别、请求 ID、用户 ID
3. WHEN 配置日志轮转 THEN THE System SHALL 按日期或大小自动切割日志文件
4. WHEN 查询日志 THEN THE System SHALL 支持按时间范围、关键字、日志级别过滤
5. WHEN 发生错误 THEN THE System SHALL 记录完整的堆栈信息和请求上下文
6. WHEN 配置日志告警 THEN THE System SHALL 在错误日志频率异常时发送告警

