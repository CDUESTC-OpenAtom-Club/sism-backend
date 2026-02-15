# Roadmap to 100% Test Pass Rate

**Current Status**: 66% pass rate (401/604 tests passing)
**Target**: 100% pass rate (604/604 tests passing)
**Gap**: 203 tests need fixing
**Last Updated**: 2026-02-14

---

## Executive Summary

This document provides a realistic, actionable roadmap to achieve 100% test pass rate for the SISM backend. The approach is prioritized by impact and effort, with clear time estimates for each phase.

**Key Insight**: The 203 failing tests fall into 3 main categories with different root causes and solutions.

---

## Current Test Results (2026-02-14)

```
Total Tests: 604
Passing: 401 (66%)
Failing: 121 (20%)
Errors: 82 (14%)
Skipped: 1 (0%)
```

### Breakdown by Category

| Category | Count | % of Total | Root Cause |
|----------|-------|------------|------------|
| **Passing Tests** | 401 | 66% | ✅ Working correctly |
| **Controller Auth Failures** | 121 | 20% | 403 Forbidden - JWT auth in tests |
| **Property Test Data Errors** | 82 | 14% | Empty database - no test data |
| **Skipped** | 1 | 0% | Intentionally disabled |

---

## Implementation Status (2026-02-14)

### Phase 1: Controller Authentication - 🔄 IN PROGRESS

**Progress**: Analysis complete, implementation started
**Status**: Encountered technical complexity, documented 3 solution options

**Work Completed**:
- ✅ Root cause analysis completed
- ✅ Proof of concept with AuthControllerIntegrationTest
- ✅ Comprehensive implementation guide created
- ✅ 3 solution options documented with pros/cons
- 🔄 Debugging authentication issues in test environment

**Solution Options**:
1. **Option A**: Complete @WithMockUser approach (2-3 hours, Spring Security best practice)
2. **Option B**: Use TestSecurityConfig to bypass auth (1-1.5 hours, simpler)
3. **Option C**: Hybrid approach (1.5-2 hours, pragmatic - recommended)

**Recommended Approach**: Option C (Hybrid)
- Use TestSecurityConfig for 8 non-auth controllers → 113 tests fixed quickly
- Fix AuthController separately with proper mocking → 8 tests fixed
- Total: 121 tests fixed in 1.5-2 hours

---

## Phase 1: Fix Controller Authentication (121 tests) ⭐ HIGH IMPACT

**Estimated Effort**: 2-3 hours
**Impact**: +20% pass rate (from 66% to 86%)
**Priority**: P0 - Critical
**Status**: 🔄 IN PROGRESS

### Problem

All controller integration tests are failing with `403 Forbidden` because:
1. Tests are trying to use JWT authentication
2. JWT filter is active in test environment
3. Mock authentication not properly configured### Solution Options

#### Option A: Use @WithMockUser (RECOMMENDED)
- Add `@WithMockUser` annotation to all controller test methods
- Simple, standard Spring Security testing approach
- No changes to security configuration needed

**Implementation**:
```java
@Test
@WithMockUser(username = "testuser", roles = {"USER"})
void shouldReturnData() throws Exception {
    mockMvc.perform(get("/api/endpoint"))
            .andExpect(status().isOk());
}
```

**Files to Update** (8 controller test files):
1. `OrgControllerIntegrationTest.java` - 6 tests
2. `AuthControllerIntegrationTest.java` - 8 tests  
3. `IndicatorControllerIntegrationTest.java` - 12 tests
4. `MilestoneControllerIntegrationTest.java` - 12 tests
5. `TaskControllerIntegrationTest.java` - 6 tests
6. `ReportControllerIntegrationTest.java` - 6 tests
7. `AdhocTaskControllerIntegrationTest.java` - 6 tests
8. `AlertControllerIntegrationTest.java` - 7 tests
9. `AuditLogControllerIntegrationTest.java` - 6 tests

**Total**: ~70 test methods need `@WithMockUser` annotation

#### Option B: Disable Security in Test Profile
- Add `@Profile("!test")` to `SecurityConfig.java`
- Create test-specific security config that permits all
- More invasive but affects all tests globally

**Trade-offs**:
- Option A: More work (70 annotations) but safer, standard approach
- Option B: Less work but changes security behavior globally in tests

**Recommendation**: Use Option A (@WithMockUser) - it's the Spring Security best practice

### Execution Steps

1. Add import: `import org.springframework.security.test.context.support.WithMockUser;`
2. Add `@WithMockUser` to each test method that makes authenticated requests
3. Keep unauthenticated tests without the annotation (they should still get 403)
4. Run tests to verify: `mvn test -Dtest=OrgControllerIntegrationTest`

### Expected Outcome

- 121 controller tests will pass
- Pass rate increases from 66% to 86%
- Build time: ~40 seconds

---

## Phase 2: Fix Property-Based Test Data (82 tests) ⭐ MEDIUM IMPACT

**Estimated Effort**: 3-4 hours
**Impact**: +14% pass rate (from 86% to 100%)
**Priority**: P1 - High

### Problem

Property-based tests are failing with `AssumptionViolatedException: Expecting actual not to be empty` because:
1. Tests assume database has existing data
2. H2 test database starts empty
3. Tests use `assumeThat(data).isNotEmpty()` which skips test if no data

### Solution

Add `@BeforeEach` setup methods to populate test data using `TestDataFactory` pattern.

**Pattern** (already established in `OrgServiceTest.java`):
```java
@BeforeEach
void setUp() {
    // Create test organizations
    testOrg = TestDataFactory.createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT);
    
    // Create test cycle
    testCycle = TestDataFactory.createTestCycle(cycleRepository, "2026年度", 2026);
    
    // Create test indicator
    testIndicator = TestDataFactory.createTestIndicator(
        indicatorRepository, testCycle, testOrg, "测试指标"
    );
}
```

### Files to Update (15 property test files)

1. **SoftDeletionBehaviorPropertyTest.java** (5 tests)
   - Add setup: Create 10 active indicators
   - Tests verify soft delete behavior

2. **ReportStatusStateMachinePropertyTest.java** (17 tests)
   - Add setup: Create indicators, milestones, reports in various states
   - Tests verify report status transitions

3. **MilestoneAdhocTaskMutualExclusionPropertyTest.java** (5 tests)
   - Add setup: Create indicators, milestones, adhoc tasks
   - Tests verify mutual exclusion rules

4. **SingleFinalVersionPropertyTest.java** (5 tests)
   - Add setup: Create indicators, milestones, multiple reports
   - Tests verify final version logic

5. **IndicatorCrudAuditPropertyTest.java** (5 tests)
   - Add setup: Create indicators for CRUD operations
   - Tests verify audit log generation

6. **AuditLogCompletenessPropertyTest.java** (9 tests)
   - Add setup: Create entities for audit operations
   - Tests verify audit log completeness

7. **UnauthorizedAccessRejectionPropertyTest.java** (5 tests)
   - Add setup: Create test users and indicators
   - Tests verify authorization checks

8. **AuthenticationVerificationPropertyTest.java** (4 tests)
   - Add setup: Create test users with tokens
   - Tests verify authentication

9. **ApprovalMilestoneStatusUpdatePropertyTest.java** (5 tests)
   - Add setup: Create milestones and reports
   - Tests verify milestone status updates

10. **AdhocTaskScopeTypePropertyTest.java** (6 tests)
    - Add setup: Create organizations and adhoc tasks
    - Tests verify scope type behavior

11. **EnumValueConsistencyPropertyTest.java** (5 tests)
    - **SKIP**: These tests query PostgreSQL enums, won't work on H2
    - Mark as `@Disabled` with explanation

### Execution Steps

1. For each property test file:
   - Add `@BeforeEach void setUp()` method
   - Use `TestDataFactory` to create required test data
   - Follow pattern from `OrgServiceTest.java`

2. For enum consistency tests:
   - Add `@Disabled("Requires PostgreSQL - H2 doesn't support native enums")`
   - Document in test class JavaDoc

3. Run tests incrementally:
   ```bash
   mvn test -Dtest=SoftDeletionBehaviorPropertyTest
   mvn test -Dtest=ReportStatusStateMachinePropertyTest
   # ... etc
   ```

### Expected Outcome

- 77 property tests will pass (82 - 5 disabled enum tests)
- Pass rate increases from 86% to 99%
- 5 enum tests remain disabled (acceptable - H2 limitation)

---

## Phase 3: Fix Field Coverage Issues (remaining failures)

**Estimated Effort**: 1-2 hours
**Impact**: +1% pass rate (from 99% to 100%)
**Priority**: P2 - Medium

### Problem

Some tests expect fields in VOs/Entities that don't exist:
- `IndicatorVO` missing fields: `responsiblePerson`, `unit`, `targetValue`, etc.
- `Indicator` entity field type mismatches

### Solution

Update VOs and Entities to match test expectations.

### Files to Update

1. **IndicatorVO.java**
   - Add missing fields from frontend requirements
   - Fields: `responsiblePerson`, `unit`, `targetValue`, `actualValue`, etc.

2. **Indicator.java**
   - Fix field type: `progressApprovalStatus` should be enum, not String
   - Add missing fields if needed

3. **IndicatorService.java**
   - Update `toIndicatorVO()` method to populate new fields

### Execution Steps

1. Run failing tests to identify exact missing fields:
   ```bash
   mvn test -Dtest=IndicatorVOFieldCoveragePropertyTest
   mvn test -Dtest=ApiResponseFieldMatchPropertyTest
   ```

2. Add missing fields to VO/Entity

3. Update service mapping methods

4. Re-run tests to verify

### Expected Outcome

- All field coverage tests pass
- Pass rate reaches 100%

---

## Summary: Path to 100%

| Phase | Effort | Impact | Cumulative Pass Rate |
|-------|--------|--------|---------------------|
| **Current** | - | - | 66% (401/604) |
| **Phase 1: Auth** | 2-3 hours | +121 tests | 86% (522/604) |
| **Phase 2: Data** | 3-4 hours | +77 tests | 99% (599/604) |
| **Phase 3: Fields** | 1-2 hours | +5 tests | 100% (604/604) |
| **Total** | **6-9 hours** | **+203 tests** | **100%** |

---

## Realistic Timeline

### Option A: Full 100% (6-9 hours)
- Day 1 (3 hours): Phase 1 - Fix all controller auth
- Day 2 (4 hours): Phase 2 - Fix property test data
- Day 3 (2 hours): Phase 3 - Fix field coverage
- **Result**: 100% pass rate

### Option B: Pragmatic 95% (4-5 hours)
- Day 1 (3 hours): Phase 1 - Fix all controller auth (86%)
- Day 2 (2 hours): Phase 2 - Fix top 5 property tests (95%)
- **Result**: 95% pass rate, remaining 5% documented as "needs data setup"

### Option C: Quick Win 86% (2-3 hours)
- Day 1 (3 hours): Phase 1 only - Fix all controller auth
- **Result**: 86% pass rate, +20% improvement

---

## Recommendation

**Start with Phase 1 (Controller Auth)** - it's the highest impact for the least effort.

**Why Phase 1 First**:
1. Fixes 121 tests (20% of total)
2. Only 2-3 hours of work
3. Standard Spring Security pattern
4. No complex data setup needed
5. Immediate visible progress

**After Phase 1**:
- Evaluate remaining time/priority
- Phase 2 can be done incrementally (one property test file at a time)
- Phase 3 is quick once you know which fields are missing

---

## Implementation Guide

### Quick Start: Fix One Controller Test File

```bash
# 1. Open a controller test file
vim src/test/java/com/sism/controller/OrgControllerIntegrationTest.java

# 2. Add import at top
import org.springframework.security.test.context.support.WithMockUser;

# 3. Add annotation to each test method
@Test
@WithMockUser(username = "testuser", roles = {"USER"})
void testMethod() { ... }

# 4. Run tests
mvn test -Dtest=OrgControllerIntegrationTest

# 5. Verify all tests pass
# Expected: 6/6 tests passing
```

### Repeat for All 8 Controller Test Files

Once you've fixed one file and verified it works, repeat the pattern for the remaining 7 controller test files.

---

## Monitoring Progress

### Check Overall Pass Rate
```bash
mvn test -DskipTests=false 2>&1 | grep "Tests run:"
```

### Check Specific Test Suite
```bash
mvn test -Dtest=OrgControllerIntegrationTest
mvn test -Dtest=SoftDeletionBehaviorPropertyTest
```

### Generate Coverage Report
```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

---

## Conclusion

Achieving 100% test pass rate is feasible with 6-9 hours of focused work. The roadmap is clear:

1. **Phase 1 (2-3h)**: Fix controller authentication → 86% pass rate
2. **Phase 2 (3-4h)**: Add property test data → 99% pass rate  
3. **Phase 3 (1-2h)**: Fix field coverage → 100% pass rate

**Next Action**: Start with Phase 1 - add `@WithMockUser` to controller tests.

---

*Document Created*: 2026-02-14
*Current Pass Rate*: 66% (401/604)
*Target Pass Rate*: 100% (604/604)
*Estimated Effort*: 6-9 hours
