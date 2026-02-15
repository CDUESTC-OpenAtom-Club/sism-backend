# Dependency Graph - Backend Architecture Refactoring

**Inventory Date**: 2026-02-13  
**Purpose**: Document component dependencies and relationships in the SISM backend  
**Scope**: Service layer dependencies

---

## Overview

This document provides a high-level view of component dependencies in the SISM backend architecture. The focus is on service layer dependencies, as these represent the core business logic relationships.

---

## Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│  (REST API endpoints, request/response handling)         │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Service Layer                        │
│  (Business logic, transaction management)                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Repository Layer                       │
│  (Data access, JPA operations)                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Entity Layer                         │
│  (Domain models, database mappings)                      │
└─────────────────────────────────────────────────────────┘
```

---

## Core Service Dependencies

### Authentication Flow

```
AuthController
    ↓
AuthService
    ├── UserRepository
    ├── SysUserRepository
    ├── RefreshTokenService
    │   ├── RefreshTokenRepository
    │   └── UserRepository
    ├── JwtUtil
    └── TokenBlacklistService (in-memory)
```

**Key Dependencies**:
- AuthService depends on 5 components
- RefreshTokenService is a sub-dependency
- JwtUtil provides token generation/validation
- TokenBlacklistService manages revoked tokens

---

### Indicator Management Flow

```
IndicatorController
    ↓
IndicatorService
    ├── IndicatorRepository
    ├── UserRepository
    ├── OrgRepository
    └── AuditLogService
        ├── AuditLogRepository
        └── UserRepository
```

**Key Dependencies**:
- IndicatorService depends on 4 components
- AuditLogService is a sub-dependency for audit trail
- Cross-references User and Organization entities

---

### Report & Approval Flow

```
ReportController
    ↓
ReportService
    ├── ReportRepository (ProgressReport)
    ├── IndicatorRepository
    ├── UserRepository
    └── ApprovalService
        ├── ApprovalRecordRepository
        ├── UserRepository
        ├── IndicatorRepository
        └── ReportRepository
```

**Key Dependencies**:
- ReportService depends on 4 components
- ApprovalService is a sub-dependency for workflow
- Circular reference between ReportService and ApprovalService (managed via interfaces)

---

### Task Management Flow

```
TaskController
    ↓
TaskService
    ├── TaskRepository (StrategicTask)
    ├── IndicatorRepository
    ├── UserRepository
    └── OrgRepository
```

**Key Dependencies**:
- TaskService depends on 4 components
- Links tasks to indicators, users, and organizations

---

### Milestone Tracking Flow

```
MilestoneController
    ↓
MilestoneService
    ├── MilestoneRepository
    ├── IndicatorRepository
    └── AlertService
        ├── AlertEventRepository
        ├── AlertRuleRepository
        └── UserRepository
```

**Key Dependencies**:
- MilestoneService depends on 3 components
- AlertService is a sub-dependency for delay warnings
- Monitors milestone progress and triggers alerts

---

### Alert Monitoring Flow

```
AlertSchedulerService (scheduled tasks)
    ├── MilestoneRepository
    ├── ReportRepository
    ├── IndicatorRepository
    └── AlertService
        ├── AlertEventRepository
        ├── AlertRuleRepository
        └── UserRepository
```

**Key Dependencies**:
- AlertSchedulerService depends on 4 components
- Runs scheduled checks for delayed milestones and overdue reports
- Creates alert events via AlertService

---

## Cross-Cutting Concerns

### Audit Logging

```
AuditLogAspect (AOP)
    ↓
AuditLogService
    ├── AuditLogRepository
    └── UserRepository
```

**Triggered by**: All services with @AuditableOperation annotation

**Purpose**: Automatic audit trail for all entity changes

---

### Security & Authentication

```
JwtAuthenticationFilter
    ↓
JwtUtil
    ↓
TokenBlacklistService
```

**Applied to**: All protected API endpoints

**Purpose**: JWT token validation and blacklist checking

---

### Rate Limiting

```
RateLimitFilter
    ↓
InMemoryRateLimiter (implements RateLimiter)
```

**Applied to**: All API endpoints

**Purpose**: Prevent API abuse with rate limiting

---

## Repository Usage Matrix

| Repository | Used By Services | Usage Count |
|------------|------------------|-------------|
| UserRepository | AuthService, IndicatorService, ReportService, ApprovalService, AlertService, AuditLogService, RefreshTokenService | 7 |
| IndicatorRepository | IndicatorService, TaskService, MilestoneService, ReportService, ApprovalService, AlertSchedulerService | 6 |
| OrgRepository | IndicatorService, TaskService, AdhocTaskService, UserService | 4 |
| ReportRepository | ReportService, ApprovalService, AlertSchedulerService | 3 |
| MilestoneRepository | MilestoneService, AlertSchedulerService | 2 |
| TaskRepository | TaskService | 1 |
| AuditLogRepository | AuditLogService | 1 |
| ApprovalRecordRepository | ApprovalService | 1 |
| AlertEventRepository | AlertService | 1 |
| RefreshTokenRepository | RefreshTokenService | 1 |

**Most Used Repositories**:
1. UserRepository (7 services) - Central to authentication and audit
2. IndicatorRepository (6 services) - Core business entity
3. OrgRepository (4 services) - Organization hierarchy

---

## Dependency Complexity Analysis

### Simple Services (1-2 dependencies)
- AssessmentCycleService (1 dependency)
- OrgService (1 dependency)
- SysOrgService (1 dependency)
- IdempotencyService (1 dependency)
- TokenBlacklistService (0 dependencies)
- InMemoryRateLimiter (0 dependencies)

**Total**: 6 services (33%)

---

### Medium Services (3-4 dependencies)
- IndicatorService (4 dependencies)
- TaskService (4 dependencies)
- MilestoneService (3 dependencies)
- ReportService (4 dependencies)
- AdhocTaskService (4 dependencies)
- ApprovalService (4 dependencies)
- AlertService (3 dependencies)
- AuditLogService (2 dependencies)
- UserService (3 dependencies)
- RefreshTokenService (2 dependencies)

**Total**: 10 services (56%)

---

### Complex Services (5+ dependencies)
- AuthService (5 dependencies)
- AlertSchedulerService (4 direct + AlertService sub-dependency)

**Total**: 2 services (11%)

---

## Circular Dependencies

### Identified Circular References

1. **ReportService ↔ ApprovalService**
   - ReportService depends on ApprovalService for approval workflow
   - ApprovalService depends on ReportRepository for report validation
   - **Resolution**: Managed via interface abstraction and lazy loading

2. **IndicatorService ↔ AuditLogService**
   - IndicatorService depends on AuditLogService for audit trail
   - AuditLogService may query IndicatorRepository for context
   - **Resolution**: One-way dependency (AuditLogService doesn't depend on IndicatorService)

**Status**: ✅ No problematic circular dependencies detected

---

## Missing Dependencies (For New Entities)

### AttachmentService (Not Implemented)
**Expected Dependencies**:
- AttachmentRepository (not yet created)
- UserRepository (for uploader tracking)
- File storage service (to be determined)

---

### AuditFlowService (Not Implemented)
**Expected Dependencies**:
- AuditFlowDefRepository (created)
- AuditStepDefRepository (created)

---

### AuditInstanceService (Not Implemented)
**Expected Dependencies**:
- AuditInstanceRepository (created)
- AuditFlowDefRepository (created)
- UserRepository (for initiator tracking)

---

### WarnLevelService (Not Implemented)
**Expected Dependencies**:
- WarnLevelRepository (created)

---

### PlanService (Not Implemented)
**Expected Dependencies**:
- PlanRepository (exists)
- AssessmentCycleRepository (exists)
- OrgRepository (exists)

---

## Dependency Injection Pattern

All services use **constructor injection** for dependencies:

```java
@Service
public class IndicatorService {
    private final IndicatorRepository indicatorRepository;
    private final UserRepository userRepository;
    private final OrgRepository orgRepository;
    private final AuditLogService auditLogService;
    
    public IndicatorService(
        IndicatorRepository indicatorRepository,
        UserRepository userRepository,
        OrgRepository orgRepository,
        AuditLogService auditLogService
    ) {
        this.indicatorRepository = indicatorRepository;
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.auditLogService = auditLogService;
    }
}
```

**Benefits**:
- Immutable dependencies
- Easy to test (mock injection)
- Clear dependency declaration
- Spring Boot best practice

---

## Recommendations

### Immediate Actions

1. **Document New Service Dependencies**:
   - When implementing AttachmentService, AuditFlowService, etc.
   - Update this document with actual dependencies
   - **Priority**: High

2. **Monitor Circular Dependencies**:
   - Watch for circular references when adding new services
   - Use interface abstraction to break cycles if needed
   - **Priority**: Medium

### Long-term Actions

3. **Consider Service Interfaces**:
   - Extract interfaces for all services (not just RateLimiter)
   - Improves testability and follows SOLID principles
   - **Priority**: Low

4. **Dependency Visualization**:
   - Generate automated dependency graphs using tools
   - Consider using ArchUnit for dependency rule enforcement
   - **Priority**: Low

---

## Conclusion

**Key Findings**:
1. ✅ Most services have 3-4 dependencies (medium complexity)
2. ✅ No problematic circular dependencies
3. ✅ UserRepository and IndicatorRepository are central to the system
4. ✅ Consistent use of constructor injection
5. ⚠️ New services will add 5+ new dependency relationships

**Architecture Health**: ✅ Good - Dependencies are well-managed and follow best practices

---

*Inventory Date: 2026-02-13*  
*Author: Backend Architecture Refactoring Team*  
*Version: 1.0*
