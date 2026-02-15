# Service Inventory - Backend Architecture Refactoring

**Inventory Date**: 2026-02-13  
**Purpose**: Baseline documentation of all service classes in the SISM backend  
**Total Services**: 18

---

## Service Summary

| Category | Count | Status |
|----------|-------|--------|
| Core Business Services | 9 | ✅ Active |
| Authentication & Security Services | 4 | ✅ Active |
| Alert & Monitoring Services | 3 | ✅ Active |
| Infrastructure Services | 2 | ✅ Active |
| **Total** | **18** | **✅ All Active** |

---

## Core Business Services

### 1. IndicatorService
**File**: `service/IndicatorService.java`  
**Purpose**: Strategic indicator management  
**Key Methods**:
- createIndicator(IndicatorCreateRequest) → IndicatorVO
- updateIndicator(Long id, IndicatorUpdateRequest) → IndicatorVO
- getIndicatorById(Long id) → IndicatorVO
- listIndicators(filters, Pageable) → Page<IndicatorVO>
- deleteIndicator(Long id) → void
- assignIndicator(Long id, Long userId) → IndicatorVO

**Dependencies**: IndicatorRepository, UserRepository, OrgRepository, AuditLogService

**Status**: ✅ Active, fully implemented with tests

---

### 2. TaskService
**File**: `service/TaskService.java`  
**Purpose**: Strategic task management  
**Key Methods**:
- createTask(TaskCreateRequest) → TaskVO
- updateTask(Long id, TaskUpdateRequest) → TaskVO
- getTaskById(Long id) → TaskVO
- listTasksByIndicator(Long indicatorId) → List<TaskVO>
- assignTask(Long id, Long userId, Long orgId) → TaskVO

**Dependencies**: TaskRepository, IndicatorRepository, UserRepository, OrgRepository

**Status**: ✅ Active, fully implemented

---

### 3. MilestoneService
**File**: `service/MilestoneService.java`  
**Purpose**: Milestone tracking and management  
**Key Methods**:
- createMilestone(MilestoneCreateRequest) → MilestoneVO
- updateMilestone(Long id, MilestoneUpdateRequest) → MilestoneVO
- getMilestoneById(Long id) → MilestoneVO
- listMilestonesByIndicator(Long indicatorId) → List<MilestoneVO>
- updateMilestoneProgress(Long id, BigDecimal progress) → MilestoneVO

**Dependencies**: MilestoneRepository, IndicatorRepository, AlertService

**Status**: ✅ Active, fully implemented with tests

---

### 4. ReportService
**File**: `service/ReportService.java`  
**Purpose**: Progress report management  
**Key Methods**:
- createReport(ReportCreateRequest) → ReportVO
- updateReport(Long id, ReportUpdateRequest) → ReportVO
- getReportById(Long id) → ReportVO
- listReportsByIndicator(Long indicatorId) → List<ReportVO>
- submitReport(Long id) → ReportVO
- approveReport(Long id, ApprovalRequest) → ReportVO

**Dependencies**: ReportRepository, IndicatorRepository, UserRepository, ApprovalService

**Status**: ✅ Active, fully implemented

---

### 5. AssessmentCycleService
**File**: `service/AssessmentCycleService.java`  
**Purpose**: Assessment cycle management  
**Key Methods**:
- createCycle(CycleCreateRequest) → AssessmentCycleVO
- updateCycle(Long id, CycleUpdateRequest) → AssessmentCycleVO
- getCycleById(Long id) → AssessmentCycleVO
- listCycles(Pageable) → Page<AssessmentCycleVO>
- getCurrentCycle() → AssessmentCycleVO

**Dependencies**: AssessmentCycleRepository

**Status**: ✅ Active, fully implemented

**Note**: Candidate for rename to `CycleService`

---

### 6. AdhocTaskService
**File**: `service/AdhocTaskService.java`  
**Purpose**: Ad-hoc task management  
**Key Methods**:
- createAdhocTask(AdhocTaskCreateRequest) → AdhocTaskVO
- updateAdhocTask(Long id, AdhocTaskUpdateRequest) → AdhocTaskVO
- getAdhocTaskById(Long id) → AdhocTaskVO
- listAdhocTasks(filters, Pageable) → Page<AdhocTaskVO>
- assignTargets(Long id, List<Long> targetIds) → AdhocTaskVO

**Dependencies**: AdhocTaskRepository, AdhocTaskTargetRepository, UserRepository, OrgRepository

**Status**: ✅ Active, fully implemented

---

### 7. ApprovalService
**File**: `service/ApprovalService.java`  
**Purpose**: Approval workflow management  
**Key Methods**:
- submitForApproval(String entityType, Long entityId) → ApprovalRecordVO
- approveEntity(Long recordId, ApprovalRequest) → ApprovalRecordVO
- rejectEntity(Long recordId, ApprovalRequest) → ApprovalRecordVO
- getApprovalHistory(String entityType, Long entityId) → List<ApprovalRecordVO>

**Dependencies**: ApprovalRecordRepository, UserRepository, IndicatorRepository, ReportRepository

**Status**: ✅ Active, fully implemented

---

### 8. OrgService
**File**: `service/OrgService.java`  
**Purpose**: Organization management (legacy)  
**Key Methods**:
- getOrgById(Long id) → OrgVO
- listOrgs() → List<OrgVO>
- getOrgTree() → OrgTreeVO

**Dependencies**: OrgRepository

**Status**: ⚠️ Active but deprecated (use SysOrgService instead)

---

### 9. SysOrgService
**File**: `service/SysOrgService.java`  
**Purpose**: Organization hierarchy management (current)  
**Key Methods**:
- getOrgById(Long id) → SysOrgVO
- listOrgs() → List<SysOrgVO>
- getOrgTree() → OrgTreeVO
- getChildOrgs(Long parentId) → List<SysOrgVO>

**Dependencies**: SysOrgRepository

**Status**: ✅ Active, fully implemented

---

## Authentication & Security Services

### 10. AuthService
**File**: `service/AuthService.java`  
**Purpose**: User authentication and authorization  
**Key Methods**:
- login(LoginRequest) → LoginResponse
- logout(String token) → void
- refreshToken(String refreshToken) → LoginResponse
- validateToken(String token) → boolean
- getCurrentUser() → UserVO

**Dependencies**: UserRepository, SysUserRepository, RefreshTokenService, JwtUtil, TokenBlacklistService

**Status**: ✅ Active, fully implemented with tests

---

### 11. UserService
**File**: `service/UserService.java`  
**Purpose**: User management  
**Key Methods**:
- getUserById(Long id) → UserVO
- getUserByUsername(String username) → UserVO
- listUsers(filters, Pageable) → Page<UserVO>
- updateUser(Long id, UserUpdateRequest) → UserVO

**Dependencies**: UserRepository, SysUserRepository, OrgRepository

**Status**: ✅ Active, fully implemented

---

### 12. RefreshTokenService
**File**: `service/RefreshTokenService.java`  
**Purpose**: JWT refresh token management  
**Key Methods**:
- createRefreshToken(Long userId) → RefreshToken
- validateRefreshToken(String token) → RefreshToken
- deleteRefreshToken(String token) → void

**Dependencies**: RefreshTokenRepository, UserRepository

**Status**: ✅ Active, fully implemented

---

### 13. TokenBlacklistService
**File**: `util/TokenBlacklistService.java`  
**Purpose**: JWT token blacklist management  
**Key Methods**:
- addToBlacklist(String token) → void
- isBlacklisted(String token) → boolean

**Dependencies**: None (in-memory cache)

**Status**: ✅ Active, fully implemented

---

## Alert & Monitoring Services

### 14. AlertService
**File**: `service/AlertService.java`  
**Purpose**: Alert event management  
**Key Methods**:
- createAlert(AlertEventVO) → AlertEvent
- getAlertById(Long id) → AlertEventVO
- listAlerts(filters, Pageable) → Page<AlertEventVO>
- acknowledgeAlert(Long id) → AlertEventVO

**Dependencies**: AlertEventRepository, AlertRuleRepository, UserRepository

**Status**: ✅ Active, fully implemented

---

### 15. AlertSchedulerService
**File**: `service/AlertSchedulerService.java`  
**Purpose**: Scheduled alert checking  
**Key Methods**:
- checkDelayedMilestones() → void (scheduled)
- checkOverdueReports() → void (scheduled)

**Dependencies**: MilestoneRepository, ReportRepository, AlertService

**Status**: ✅ Active, fully implemented

---

### 16. AuditLogService
**File**: `service/AuditLogService.java`  
**Purpose**: Audit trail management  
**Key Methods**:
- logAction(AuditAction, String entityType, Long entityId, Object oldValue, Object newValue) → void
- getAuditLogs(String entityType, Long entityId) → List<AuditLogVO>

**Dependencies**: AuditLogRepository, UserRepository

**Status**: ✅ Active, fully implemented

---

## Infrastructure Services

### 17. IdempotencyService
**File**: `service/IdempotencyService.java`  
**Purpose**: Idempotency key management  
**Key Methods**:
- checkIdempotency(String key, String requestHash) → Optional<IdempotencyRecord>
- saveIdempotencyRecord(String key, String requestHash, String responseBody, int statusCode) → void

**Dependencies**: IdempotencyRepository

**Status**: ✅ Active, fully implemented

---

### 18. RateLimiter + InMemoryRateLimiter
**Files**: `service/RateLimiter.java` (interface), `service/InMemoryRateLimiter.java` (impl)  
**Purpose**: Rate limiting for API endpoints  
**Key Methods**:
- tryAcquire(String key) → boolean
- reset(String key) → void

**Dependencies**: None (in-memory)

**Status**: ✅ Active, fully implemented

---

## Missing Services (Identified in Design Document)

### PlanService (Not Implemented)
**Proposed File**: `service/PlanService.java`  
**Purpose**: Strategic plan management  
**Status**: ❌ Not implemented

---

### AttachmentService (Not Implemented)
**Proposed File**: `service/AttachmentService.java`  
**Purpose**: File attachment management  
**Status**: ❌ Not implemented (Attachment entity created in Task 2.1)

---

### AuditFlowService (Not Implemented)
**Proposed File**: `service/AuditFlowService.java`  
**Purpose**: Audit workflow definition management  
**Status**: ❌ Not implemented (AuditFlowDef entity created in Task 2.2)

---

### AuditInstanceService (Not Implemented)
**Proposed File**: `service/AuditInstanceService.java`  
**Purpose**: Audit workflow instance management  
**Status**: ❌ Not implemented (AuditInstance entity created in Task 2.2)

---

### WarnLevelService (Not Implemented)
**Proposed File**: `service/WarnLevelService.java`  
**Purpose**: Warning level configuration management  
**Status**: ❌ Not implemented (WarnLevel entity created in Task 2.3)

---

## Conclusion

**Key Findings**:
1. ✅ 18 active services covering core business logic
2. ⚠️ 5 services missing for new entities
3. ✅ Consistent service architecture patterns
4. ⚠️ Some services lack unit tests

**Next Steps**: Implement services for new entities (Phase 2)

---

*Inventory Date: 2026-02-13*  
*Author: Backend Architecture Refactoring Team*  
*Version: 1.0*
