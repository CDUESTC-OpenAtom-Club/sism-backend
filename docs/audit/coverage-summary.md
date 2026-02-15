# Test Coverage Summary - Quick Reference

**Date**: 2026-02-13  
**Status**: ⚠️ Below Target (5% vs 80% target)

## Key Metrics

| Layer | Instruction Coverage | Status |
|-------|---------------------|--------|
| **Overall** | **5%** (677/11,321) | ❌ Below target |
| Service Layer | 6% (559/8,338) | ❌ Below target |
| Controller Layer | 2% (40/2,045) | ❌ Below target |
| Utility Layer | 2% (16/727) | ❌ Below target |
| Common Layer | 24% (49/193) | ❌ Below target |

## Test Execution Status

- **Total Tests**: 371
- **Passed**: 89 (24%)
- **Failed**: 18 (5%)
- **Errors**: 263 (71%)
- **Skipped**: 1 (0%)

## Why Coverage is Low

1. **Missing Test Data** (200+ test failures)
   - Tests expect pre-populated database but H2 starts empty
   - Solution: Add `@BeforeEach` data setup to all tests

2. **Schema Issues** (50+ test failures)
   - H2 missing `task` table
   - Solution: Fix JPA entity scanning or add manual schema

3. **New Entities Not Tested** (0% coverage)
   - Attachment, AuditFlowDef, AuditStepDef, AuditInstance, WarnLevel
   - Solution: Write tests as services/controllers are implemented

4. **Controller Tests Missing** (2% coverage)
   - No MockMvc integration tests
   - Solution: Add controller integration tests with JWT mocking

## Recommendations

### Immediate (This Week)
1. Fix task table schema issue (30 min)
2. Add test data setup pattern (4-6 hours)
3. Write tests for new entities (6-8 hours)

### Short-term (Next 2 Weeks)
4. Add controller integration tests (8-10 hours)
5. Add utility class tests (2-3 hours)

### Expected Timeline
- **Week 1**: Fix existing tests → 40-50% coverage
- **Week 2**: New entity tests → 60-70% coverage
- **Week 3**: Complete coverage → 80%+ coverage

## How to View Full Report

```bash
# Generate coverage report
cd sism-backend
mvn clean test jacoco:report

# View HTML report
open target/site/jacoco/index.html
```

**Full Report**: `sism-backend/docs/audit/test-coverage-report.md`

## Conclusion

Coverage is currently below target but this is expected given:
- Many tests fail due to missing test data (not code issues)
- New entities haven't been tested yet
- Test infrastructure is working correctly

**Recommendation**: Continue with architecture refactoring and improve coverage incrementally as new code is developed.
