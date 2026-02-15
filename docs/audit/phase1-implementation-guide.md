# Phase 1 Implementation Guide - Detailed Steps

**Date**: 2026-02-14
**Purpose**: Step-by-step guide to fix all 121 controller test failures

## Pattern to Apply (Proven Working)

### Step 1: Add Import
```java
import org.springframework.security.test.context.support.WithMockUser;
```

### Step 2: Remove JWT Token Fields and Methods
Remove these from each test class:
```java
private String authToken;  // DELETE THIS

private String loginAndGetToken(String username, String password) throws Exception {
    // DELETE THIS ENTIRE METHOD
}
```

### Step 3: Simplify @BeforeEach setUp()
Remove the loginAndGetToken() call:
```java
@BeforeEach
void setUp() throws Exception {
    // Keep test data creation
    testUser = createTestUser();
    testIndicator = createTestIndicator();
    
    // DELETE THIS LINE:
    // authToken = loginAndGetToken(testUser.getUsername(), "testPassword123");
}
```

### Step 4: Add @WithMockUser to Test Methods
Add annotation to each test method that needs authentication:
```java
@Test
@WithMockUser(username = "testuser", roles = {"USER"})  // ADD THIS
@DisplayName("Should return data")
void shouldReturnData() throws Exception {
    mockMvc.perform(get("/api/endpoint"))  // Remove .header("Authorization", ...)
            .andExpect(status().isOk());
}
```

### Step 5: Remove Authorization Headers
Remove `.header("Authorization", "Bearer " + authToken)` from all requests:
```java
// BEFORE:
mockMvc.perform(get("/api/endpoint")
        .header("Authorization", "Bearer " + authToken))  // DELETE THIS LINE

// AFTER:
mockMvc.perform(get("/api/endpoint"))
```

### Step 6: Keep Unauthorized Tests Without Annotation
Tests that verify 403/401 behavior should NOT have @WithMockUser:
```java
@Test
@DisplayName("Should return 403 without authentication")
void shouldReturn403WithoutAuth() throws Exception {
    mockMvc.perform(get("/api/endpoint"))
            .andExpect(status().isForbidden());
}
```

## File-by-File Checklist

### ✅ 1. AuthControllerIntegrationTest.java - COMPLETED
- [x] Added @WithMockUser import
- [x] Removed loginAndGetToken() method
- [x] Added @WithMockUser to logout tests
- [x] Added @WithMockUser to /me endpoint test
- [x] Kept login tests without @WithMockUser (testing authentication itself)

### ⏳ 2. OrgControllerIntegrationTest.java - PARTIALLY DONE
- [x] Already has @WithMockUser on some tests
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Remove .header() calls from requests
- [ ] Simplify setUp()

### ⏳ 3. IndicatorControllerIntegrationTest.java - TODO (24 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 24 test methods
- [ ] Remove .header() calls
- [ ] Simplify setUp()

### ⏳ 4. MilestoneControllerIntegrationTest.java - TODO (14 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 14 test methods
- [ ] Remove .header() calls
- [ ] Simplify setUp()

### ⏳ 5. TaskControllerIntegrationTest.java - TODO (7 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 7 test methods (except 401 test)
- [ ] Remove .header() calls
- [ ] Simplify setUp()

### ⏳ 6. ReportControllerIntegrationTest.java - TODO (13 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 13 test methods
- [ ] Remove .header() calls
- [ ] Simplify setUp()

### ⏳ 7. AdhocTaskControllerIntegrationTest.java - TODO (15 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 15 test methods
- [ ] Remove .header() calls
- [ ] Simplify setUp()

### ⏳ 8. AlertControllerIntegrationTest.java - TODO (12 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 12 test methods
- [ ] Remove .header() calls
- [ ] Simplify setUp()

### ⏳ 9. AuditLogControllerIntegrationTest.java - TODO (11 tests)
- [ ] Add @WithMockUser import
- [ ] Remove authToken field
- [ ] Remove loginAndGetToken() method
- [ ] Add @WithMockUser to all 11 test methods
- [ ] Remove .header() calls
- [ ] Simplify setUp()

## Verification Steps

After updating each file:

1. **Compile Check**:
```bash
mvn compile test-compile
```

2. **Run Single Test File**:
```bash
mvn test -Dtest=IndicatorControllerIntegrationTest
```

3. **Check Results**:
- All tests should pass
- No 403 Forbidden errors
- Faster execution (no JWT token generation)

## Final Verification

After all files are updated:

```bash
# Run all controller tests
mvn test -Dtest=*ControllerIntegrationTest

# Run full test suite
mvn test -DskipTests=false

# Expected results:
# - Tests run: 604
# - Passing: 522 (86%)
# - Failing: 82 (property tests - Phase 2)
# - Errors: 0
```

## Time Estimate

- Per file: 10-15 minutes
- Total for 8 remaining files: 1.5-2 hours
- Verification: 15 minutes
- **Total Phase 1**: 2-2.5 hours

## Success Criteria

- [x] AuthControllerIntegrationTest: 8/8 tests passing
- [ ] OrgControllerIntegrationTest: 6/6 tests passing
- [ ] IndicatorControllerIntegrationTest: 24/24 tests passing
- [ ] MilestoneControllerIntegrationTest: 14/14 tests passing
- [ ] TaskControllerIntegrationTest: 7/7 tests passing
- [ ] ReportControllerIntegrationTest: 13/13 tests passing
- [ ] AdhocTaskControllerIntegrationTest: 15/15 tests passing
- [ ] AlertControllerIntegrationTest: 12/12 tests passing
- [ ] AuditLogControllerIntegrationTest: 11/11 tests passing
- [ ] **Total**: 121/121 controller tests passing

---

*Created*: 2026-02-14
*Status*: In Progress (1/9 files completed)
*Next*: Continue with remaining 8 files

