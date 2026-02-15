# DTO/VO Inventory - Backend Architecture Refactoring

**Inventory Date**: 2026-02-13  
**Purpose**: Baseline documentation of all DTOs (requests) and VOs (responses) in the SISM backend  
**Total DTOs**: 13  
**Total VOs**: 14

---

## Summary

| Category | DTOs | VOs | Status |
|----------|------|-----|--------|
| Indicator Module | 3 | 1 | ✅ Active |
| Task Module | 4 | 2 | ✅ Active |
| Milestone Module | 2 | 1 | ✅ Active |
| Report Module | 2 | 1 | ✅ Active |
| Organization Module | 0 | 2 | ✅ Active |
| User/Auth Module | 1 | 2 | ✅ Active |
| Approval Module | 1 | 1 | ✅ Active |
| Audit Module | 0 | 2 | ✅ Active |
| Alert Module | 0 | 1 | ✅ Active |
| Cycle Module | 0 | 1 | ✅ Active |
| **Total** | **13** | **14** | **✅ All Active** |

---

## Data Transfer Objects (DTOs) - Request Objects

### Indicator Module

#### 1. IndicatorCreateRequest
**File**: `dto/IndicatorCreateRequest.java`  
**Purpose**: Create new indicator  
**Key Fields**:
- indicatorName (String)
- indicatorCode (String)
- targetValue (String)
- year (Integer)
- orgId (Long)
- responsibleUserId (Long)

**Status**: ✅ Active

---

#### 2. IndicatorUpdateRequest
**File**: `dto/IndicatorUpdateRequest.java`  
**Purpose**: Update existing indicator  
**Key Fields**:
- indicatorName (String)
- targetValue (String)
- currentValue (String)
- status (IndicatorStatus)

**Status**: ✅ Active

---

#### 3. IndicatorAuditData
**File**: `dto/IndicatorAuditData.java`  
**Purpose**: Audit data for indicator changes  
**Key Fields**:
- indicatorId (Long)
- oldValue (String)
- newValue (String)
- changeType (String)

**Status**: ✅ Active

---

### Task Module

#### 4. TaskCreateRequest
**File**: `dto/TaskCreateRequest.java`  
**Purpose**: Create new strategic task  
**Key Fields**:
- taskName (String)
- taskType (TaskType)
- indicatorId (Long)
- assignedOrgId (Long)
- assignedUserId (Long)
- deadline (LocalDate)

**Status**: ✅ Active

---

#### 5. TaskUpdateRequest
**File**: `dto/TaskUpdateRequest.java`  
**Purpose**: Update existing task  
**Key Fields**:
- taskName (String)
- status (String)
- deadline (LocalDate)

**Status**: ✅ Active

---

#### 6. AdhocTaskCreateRequest
**File**: `dto/AdhocTaskCreateRequest.java`  
**Purpose**: Create new ad-hoc task  
**Key Fields**:
- taskName (String)
- taskDescription (String)
- scopeType (AdhocScopeType)
- deadline (LocalDate)
- targetOrgIds (List<Long>)
- targetUserIds (List<Long>)

**Status**: ✅ Active

---

#### 7. AdhocTaskUpdateRequest
**File**: `dto/AdhocTaskUpdateRequest.java`  
**Purpose**: Update existing ad-hoc task  
**Key Fields**:
- taskName (String)
- taskDescription (String)
- status (AdhocTaskStatus)
- deadline (LocalDate)

**Status**: ✅ Active

---

### Milestone Module

#### 8. MilestoneCreateRequest
**File**: `dto/MilestoneCreateRequest.java`  
**Purpose**: Create new milestone  
**Key Fields**:
- indicatorId (Long)
- milestoneName (String)
- targetDate (LocalDate)
- targetProgress (BigDecimal)

**Status**: ✅ Active

---

#### 9. MilestoneUpdateRequest
**File**: `dto/MilestoneUpdateRequest.java`  
**Purpose**: Update existing milestone  
**Key Fields**:
- milestoneName (String)
- targetDate (LocalDate)
- targetProgress (BigDecimal)
- actualProgress (BigDecimal)
- status (MilestoneStatus)

**Status**: ✅ Active

---

### Report Module

#### 10. ReportCreateRequest
**File**: `dto/ReportCreateRequest.java`  
**Purpose**: Create new progress report  
**Key Fields**:
- indicatorId (Long)
- reportedValue (String)
- reportedProgress (BigDecimal)
- reportDate (LocalDate)
- remarks (String)

**Status**: ✅ Active

---

#### 11. ReportUpdateRequest
**File**: `dto/ReportUpdateRequest.java`  
**Purpose**: Update existing report  
**Key Fields**:
- reportedValue (String)
- reportedProgress (BigDecimal)
- remarks (String)

**Status**: ✅ Active

---

### User/Auth Module

#### 12. LoginRequest
**File**: `dto/LoginRequest.java`  
**Purpose**: User login  
**Key Fields**:
- username (String)
- password (String)

**Status**: ✅ Active

---

### Approval Module

#### 13. ApprovalRequest
**File**: `dto/ApprovalRequest.java`  
**Purpose**: Approve or reject entity  
**Key Fields**:
- action (ApprovalAction)
- comments (String)

**Status**: ✅ Active

---

## Value Objects (VOs) - Response Objects

### Indicator Module

#### 1. IndicatorVO
**File**: `vo/IndicatorVO.java`  
**Purpose**: Indicator response  
**Key Fields**:
- id (Long)
- indicatorName (String)
- indicatorCode (String)
- status (IndicatorStatus)
- level (IndicatorLevel)
- targetValue (String)
- currentValue (String)
- year (Integer)
- orgId (Long)
- orgName (String)
- responsibleUserId (Long)
- responsibleUserName (String)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)

**Status**: ✅ Active

---

### Task Module

#### 2. TaskVO
**File**: `vo/TaskVO.java`  
**Purpose**: Strategic task response  
**Key Fields**:
- id (Long)
- taskName (String)
- taskType (TaskType)
- indicatorId (Long)
- indicatorName (String)
- assignedOrgId (Long)
- assignedOrgName (String)
- assignedUserId (Long)
- assignedUserName (String)
- status (String)
- deadline (LocalDate)

**Status**: ✅ Active

---

#### 3. AdhocTaskVO
**File**: `vo/AdhocTaskVO.java`  
**Purpose**: Ad-hoc task response  
**Key Fields**:
- id (Long)
- taskName (String)
- taskDescription (String)
- status (AdhocTaskStatus)
- scopeType (AdhocScopeType)
- creatorId (Long)
- creatorName (String)
- deadline (LocalDate)
- targets (List<TargetInfo>)

**Status**: ✅ Active

---

### Milestone Module

#### 4. MilestoneVO
**File**: `vo/MilestoneVO.java`  
**Purpose**: Milestone response  
**Key Fields**:
- id (Long)
- indicatorId (Long)
- indicatorName (String)
- milestoneName (String)
- targetDate (LocalDate)
- targetProgress (BigDecimal)
- actualProgress (BigDecimal)
- status (MilestoneStatus)

**Status**: ✅ Active

---

### Report Module

#### 5. ReportVO
**File**: `vo/ReportVO.java`  
**Purpose**: Progress report response  
**Key Fields**:
- id (Long)
- indicatorId (Long)
- indicatorName (String)
- reportedValue (String)
- reportedProgress (BigDecimal)
- reportDate (LocalDate)
- reporterId (Long)
- reporterName (String)
- approvalStatus (ProgressApprovalStatus)
- remarks (String)

**Status**: ✅ Active

---

### Organization Module

#### 6. OrgVO
**File**: `vo/OrgVO.java`  
**Purpose**: Organization response (legacy)  
**Key Fields**:
- id (Long)
- orgName (String)
- orgCode (String)
- orgType (OrgType)
- parentId (Long)

**Status**: ✅ Active (legacy, use SysOrgVO)

---

#### 7. SysOrgVO
**File**: `vo/SysOrgVO.java`  
**Purpose**: Organization response (current)  
**Key Fields**:
- id (Long)
- orgName (String)
- orgCode (String)
- orgType (OrgType)
- parentId (Long)
- sortOrder (Integer)

**Status**: ✅ Active

---

#### 8. OrgTreeVO
**File**: `vo/OrgTreeVO.java`  
**Purpose**: Organization tree structure  
**Key Fields**:
- id (Long)
- orgName (String)
- orgCode (String)
- orgType (OrgType)
- children (List<OrgTreeVO>)

**Status**: ✅ Active

---

### User/Auth Module

#### 9. UserVO
**File**: `vo/UserVO.java`  
**Purpose**: User response  
**Key Fields**:
- id (Long)
- username (String)
- realName (String)
- email (String)
- phone (String)
- orgId (Long)
- orgName (String)
- roles (List<String>)
- isActive (Boolean)

**Status**: ✅ Active

---

#### 10. LoginResponse
**File**: `vo/LoginResponse.java`  
**Purpose**: Login response with tokens  
**Key Fields**:
- accessToken (String)
- refreshToken (String)
- tokenType (String)
- expiresIn (Long)
- user (UserVO)

**Status**: ✅ Active

---

### Approval Module

#### 11. ApprovalRecordVO
**File**: `vo/ApprovalRecordVO.java`  
**Purpose**: Approval record response  
**Key Fields**:
- id (Long)
- entityType (String)
- entityId (Long)
- action (ApprovalAction)
- approverId (Long)
- approverName (String)
- approvalDate (LocalDateTime)
- comments (String)

**Status**: ✅ Active

---

### Audit Module

#### 12. AuditLogVO
**File**: `vo/AuditLogVO.java`  
**Purpose**: Audit log response  
**Key Fields**:
- id (Long)
- entityType (String)
- entityId (Long)
- action (AuditAction)
- userId (Long)
- username (String)
- timestamp (LocalDateTime)
- oldValue (String)
- newValue (String)

**Status**: ✅ Active

---

### Alert Module

#### 13. AlertEventVO
**File**: `vo/AlertEventVO.java`  
**Purpose**: Alert event response  
**Key Fields**:
- id (Long)
- alertType (String)
- severity (AlertSeverity)
- status (AlertStatus)
- entityType (String)
- entityId (Long)
- message (String)
- triggeredAt (LocalDateTime)

**Status**: ✅ Active

---

### Cycle Module

#### 14. AssessmentCycleVO
**File**: `vo/AssessmentCycleVO.java`  
**Purpose**: Assessment cycle response  
**Key Fields**:
- id (Long)
- cycleName (String)
- startDate (LocalDate)
- endDate (LocalDate)
- year (Integer)
- isActive (Boolean)

**Status**: ✅ Active

**Note**: Candidate for rename to `CycleVO`

---

## Missing DTOs/VOs (Identified in Design Document)

### Plan Module (Not Implemented)
- **PlanCreateRequest** (DTO) - Not implemented
- **PlanUpdateRequest** (DTO) - Not implemented
- **PlanVO** (VO) - Not implemented

**Reason**: Plan entity exists but no DTOs/VOs created yet

---

### Attachment Module (Not Implemented)
- **AttachmentUploadRequest** (DTO) - Not implemented
- **AttachmentVO** (VO) - Not implemented

**Reason**: Attachment entity created (Task 2.1) but DTOs/VOs not yet created

---

### Audit Flow Module (Not Implemented)
- **AuditFlowCreateRequest** (DTO) - Not implemented
- **AuditFlowUpdateRequest** (DTO) - Not implemented
- **AuditFlowVO** (VO) - Not implemented
- **AuditStepVO** (VO) - Not implemented

**Reason**: AuditFlowDef entity created (Task 2.2) but DTOs/VOs not yet created

---

### Warn Level Module (Not Implemented)
- **WarnLevelCreateRequest** (DTO) - Not implemented
- **WarnLevelUpdateRequest** (DTO) - Not implemented
- **WarnLevelVO** (VO) - Not implemented

**Reason**: WarnLevel entity created (Task 2.3) but DTOs/VOs not yet created

---

## DTO/VO Organization Recommendations

Based on design document analysis, DTOs and VOs should be organized by business module:

### Proposed Structure

```
dto/
├── indicator/
│   ├── IndicatorCreateRequest.java
│   ├── IndicatorUpdateRequest.java
│   └── IndicatorAuditData.java
├── task/
│   ├── TaskCreateRequest.java
│   └── TaskUpdateRequest.java
├── milestone/
│   ├── MilestoneCreateRequest.java
│   └── MilestoneUpdateRequest.java
├── report/
│   ├── ReportCreateRequest.java
│   └── ReportUpdateRequest.java
├── adhoc/
│   ├── AdhocTaskCreateRequest.java
│   └── AdhocTaskUpdateRequest.java
├── audit/
│   └── ApprovalRequest.java
└── user/
    └── LoginRequest.java

vo/
├── indicator/
│   └── IndicatorVO.java
├── task/
│   ├── TaskVO.java
│   └── AdhocTaskVO.java
├── milestone/
│   └── MilestoneVO.java
├── report/
│   └── ReportVO.java
├── org/
│   ├── OrgVO.java
│   ├── SysOrgVO.java
│   └── OrgTreeVO.java
├── user/
│   ├── UserVO.java
│   └── LoginResponse.java
├── audit/
│   ├── ApprovalRecordVO.java
│   └── AuditLogVO.java
├── alert/
│   └── AlertEventVO.java
└── plan/
    └── AssessmentCycleVO.java
```

**Status**: ⚠️ Not implemented (current structure is flat)

**Priority**: Medium (design document recommendation, Phase 5)

---

## DTO/VO Statistics

### By Implementation Status
- Fully implemented: 27 (DTOs + VOs) (100%)
- Missing (identified): 10 (DTOs + VOs) (27% of total needed)

### By Module
- Indicator: 4 (3 DTOs + 1 VO)
- Task: 6 (4 DTOs + 2 VOs)
- Milestone: 3 (2 DTOs + 1 VO)
- Report: 3 (2 DTOs + 1 VO)
- Organization: 3 (0 DTOs + 3 VOs)
- User/Auth: 3 (1 DTO + 2 VOs)
- Approval: 2 (1 DTO + 1 VO)
- Audit: 2 (0 DTOs + 2 VOs)
- Alert: 1 (0 DTOs + 1 VO)
- Cycle: 1 (0 DTOs + 1 VO)

---

## Recommendations

### Immediate Actions (P0 - Critical)

1. **Create DTOs/VOs for New Entities**:
   - Plan module (3 DTOs/VOs)
   - Attachment module (2 DTOs/VOs)
   - Audit Flow module (4 DTOs/VOs)
   - Warn Level module (3 DTOs/VOs)
   - **Effort**: 4-6 hours
   - **Priority**: High (blocks service/controller implementation)

### Short-term Actions (P1 - High)

2. **Add Validation Annotations**:
   - Add @NotNull, @Size, @Pattern to all DTOs
   - Ensure comprehensive input validation
   - **Effort**: 2-3 hours
   - **Priority**: Medium

### Long-term Actions (P2 - Medium)

3. **Organize DTOs/VOs by Module**:
   - Create subpackages for each business module
   - Move existing DTOs/VOs to new structure
   - Deprecate old locations
   - **Effort**: 4-6 hours
   - **Priority**: Low (design document recommendation, Phase 5)

---

## Conclusion

**Key Findings**:
1. ✅ 27 active DTOs/VOs covering core business logic
2. ⚠️ 10 DTOs/VOs missing for new entities
3. ⚠️ Current structure is flat (not organized by module)
4. ✅ Consistent naming conventions (Request suffix for DTOs, VO suffix for responses)

**Next Steps**:
1. Create DTOs/VOs for new entities (Phase 2)
2. Add validation annotations to all DTOs
3. Consider module-based organization (Phase 5, optional)

---

*Inventory Date: 2026-02-13*  
*Author: Backend Architecture Refactoring Team*  
*Version: 1.0*
