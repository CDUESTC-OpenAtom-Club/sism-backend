# Requirements Document

## Introduction

本需求文档定义了前后端数据对齐的标准作业程序（SOP），用于系统性地解决前端 Mock 数据与后端数据库之间的数据结构和内容不一致问题。该流程分为五个阶段：确立标准、差距分析、结构同步、内容对齐、接口放行。

## Glossary

- **Mock Data（模拟数据）**: 前端开发时使用的本地硬编码数据，用于页面开发和测试
- **Seed Data（种子数据）**: 数据库初始化时插入的基础数据
- **Schema（模式）**: 数据库表的结构定义，包括字段名、类型、约束等
- **Entity（实体）**: 后端 ORM 框架中映射数据库表的 Java/TypeScript 类
- **VO（Value Object）**: 后端返回给前端的数据传输对象
- **Gap Analysis（差距分析）**: 对比前端需求与数据库现状，识别缺失或不一致的字段
- **Data Dictionary（数据字典）**: 记录字段名、类型、业务含义的结构化文档

## Requirements

### Requirement 1: 确立标准 (The Standard)

**User Story:** As a developer, I want to extract a complete data dictionary from frontend mock data, so that I have a clear standard for what the database should provide.

#### Acceptance Criteria

1. WHEN analyzing frontend mock data THEN the system SHALL identify all field names (keys) used in the mock objects
2. WHEN analyzing frontend mock data THEN the system SHALL determine the value type for each field (String, Number, Boolean, Array, Object)
3. WHEN analyzing frontend mock data THEN the system SHALL document the business context and meaning for each field
4. WHEN the data dictionary is complete THEN the system SHALL output a structured JSON or markdown format documenting all fields

### Requirement 2: 差距分析 (Gap Analysis)

**User Story:** As a developer, I want to compare frontend requirements with database schema, so that I can identify all missing or inconsistent fields.

#### Acceptance Criteria

1. WHEN comparing frontend fields with database columns THEN the system SHALL mark fields as present (✅), missing (❌), or inconsistent (⚠️)
2. WHEN a field exists in frontend but not in database THEN the system SHALL add it to the "missing fields" list
3. WHEN a field type differs between frontend and database THEN the system SHALL add it to the "type mismatch" list
4. WHEN analysis is complete THEN the system SHALL produce a gap report listing all discrepancies

### Requirement 3: 结构同步 (Schema Sync)

**User Story:** As a database administrator, I want to update the database schema to match frontend requirements, so that the database can store all required data.

#### Acceptance Criteria

1. WHEN a field is missing from the database THEN the system SHALL generate an ALTER TABLE ADD COLUMN statement
2. WHEN a field type needs correction THEN the system SHALL generate an ALTER TABLE MODIFY COLUMN statement or document the conversion strategy
3. WHEN schema changes are applied THEN the system SHALL verify the changes do not break existing data
4. WHEN schema sync is complete THEN the system SHALL update the backend Entity classes to include new fields

### Requirement 4: 内容对齐 (Content Alignment)

**User Story:** As a developer, I want database seed data to match frontend mock data exactly, so that the system behaves consistently during development and testing.

#### Acceptance Criteria

1. WHEN inserting seed data THEN the system SHALL use exact values from frontend mock data (not placeholder values like "test" or "123")
2. WHEN inserting seed data THEN the system SHALL preserve the exact format of mock data (e.g., status values, date formats)
3. WHEN inserting seed data THEN the system SHALL maintain referential integrity with related tables
4. WHEN content alignment is complete THEN the system SHALL have a complete set of realistic test data matching mock data

### Requirement 5: 接口放行 (API Expose)

**User Story:** As a frontend developer, I want the API to return all new fields, so that the frontend can display complete data without modification.

#### Acceptance Criteria

1. WHEN backend Entity is updated THEN the system SHALL include all new database fields as Java properties
2. WHEN backend VO is updated THEN the system SHALL include all fields required by frontend TypeScript interfaces
3. WHEN API endpoint is called THEN the system SHALL return data with field names matching frontend expectations (camelCase)
4. WHEN API is deployed THEN the system SHALL pass integration tests verifying data completeness

### Requirement 6: 数据一致性验证 (Data Consistency Verification)

**User Story:** As a QA engineer, I want to verify that frontend mock data and database data are synchronized, so that testing is reliable across environments.

#### Acceptance Criteria

1. WHEN performing round-trip verification THEN serializing and deserializing data SHALL produce equivalent objects
2. WHEN comparing mock data with API response THEN all field values SHALL match exactly
3. WHEN running consistency checks THEN the system SHALL report any discrepancies between mock and database data
4. WHEN validation fails THEN the system SHALL provide clear error messages indicating which fields are mismatched

### Requirement 7: 指标数据完整性 (Indicator Data Completeness)

**User Story:** As a frontend developer, I want the database to contain all indicator types with proper milestone configurations, so that the indicator table displays complete and realistic data.

#### Acceptance Criteria

1. WHEN the database is initialized THEN the system SHALL contain at least 12 quantitative indicators (定量指标) with default 12-month milestones
2. WHEN the database is initialized THEN the system SHALL contain qualitative indicators (定性指标) with custom milestone configurations
3. WHEN displaying indicator table THEN the system SHALL show both development indicators (发展性指标) and basic indicators (基础性指标)
4. WHEN displaying indicator detail THEN the system SHALL show milestone timeline with correct progress data
5. WHEN filtering by indicator type THEN the system SHALL return correct subset of indicators matching the filter criteria

### Requirement 8: 前端页面数据覆盖 (Frontend Page Data Coverage)

**User Story:** As a product owner, I want all frontend page components to have corresponding backend data, so that every UI element displays real data instead of placeholders.

#### Acceptance Criteria

1. WHEN viewing the dashboard page THEN the system SHALL display real statistics data from the database
2. WHEN viewing the indicator list page THEN the system SHALL display all indicator fields including status, progress, responsible person, and department
3. WHEN viewing the indicator detail dialog THEN the system SHALL display milestone timeline, approval history, and audit logs
4. WHEN viewing the strategic task page THEN the system SHALL display task hierarchy with correct organizational relationships
5. WHEN using browser DevTools to inspect API responses THEN all fields required by Vue components SHALL be present in the response



### Requirement 9: 前后端连接安全 (Frontend-Backend Connection Security)

**User Story:** As a security engineer, I want all data synchronization operations to follow security best practices, so that sensitive data is protected during the alignment process.

#### Acceptance Criteria

1. WHEN connecting to the database THEN the system SHALL use encrypted connections and secure credentials
2. WHEN executing schema changes THEN the system SHALL validate SQL statements to prevent injection attacks
3. WHEN transferring data between frontend and backend THEN the system SHALL use HTTPS and proper authentication tokens
4. WHEN logging alignment operations THEN the system SHALL NOT expose sensitive information in logs

### Requirement 10: 数据库数据补充 (Database Data Completion)

**User Story:** As a developer, I want to supplement the database with missing data that matches the existing frontend logic, so that the system displays real data without changing frontend code.

#### Acceptance Criteria

1. WHEN the frontend expects data for a specific year THEN the database SHALL contain complete records for that year
2. WHEN inserting indicator data THEN the system SHALL follow the existing frontend data structure and field naming conventions
3. WHEN adding milestone data THEN the system SHALL create records that align with the frontend's timeline display logic
4. WHEN supplementing data THEN the system SHALL preserve existing frontend logic and only add missing database records

### Requirement 11: 增量分析优化 (Incremental Analysis Optimization)

**User Story:** As a developer, I want the alignment process to be efficient and avoid redundant analysis, so that I can quickly identify and fix data discrepancies.

#### Acceptance Criteria

1. WHEN performing gap analysis THEN the system SHALL cache previous analysis results to avoid re-reading unchanged files
2. WHEN comparing data THEN the system SHALL only analyze fields that have changed since the last sync
3. WHEN generating reports THEN the system SHALL highlight only new or changed discrepancies
4. WHEN executing alignment tasks THEN the system SHALL skip already-aligned data to improve performance
