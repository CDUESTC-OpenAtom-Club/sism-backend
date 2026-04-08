---
name: sism-organization deep audit report
description: Third-round deep audit report for sism-organization module
type: audit
---

# SISM-Organization Module -- Third-Round Deep Audit Report

## Audit Scope

- **Module:** `sism-organization` at `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/`
- **Related:** Shared kernel (com.sism.enums), IAM module (User-Org mapping), database schema
- **Date:** 2026-04-06
- **Method:** Full read of every source file, database schema, migration history, and test coverage

---

## HIGH SEVERITY ISSUES

### H-1. No API Endpoint for Setting Parent Organization (Broken Hierarchy Management)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/interfaces/rest/OrganizationController.java` (no PUT/PATCH for parentOrgId)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/interfaces/dto/OrgRequest.java` (has parentOrgId field)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (has updateParent() method)

**Evidence:**
The `SysOrg` entity has a `parentOrgId` field and a full `updateParent()` business method that properly handles:
- Setting parent organization
- Calculating level (parent.level + 1)
- Validation (cannot be its own parent)

```java
// SysOrg.java - Line 155
public void updateParent(SysOrg parentOrg) {
    if (parentOrg != null && parentOrg.id != null && parentOrg.id.equals(this.id)) {
        throw new IllegalArgumentException("Organization cannot be its own parent");
    }
    this.parentOrgId = parentOrg != null ? parentOrg.id : null;
    if (parentOrg != null) {
        this.level = parentOrg.level + 1;
    } else {
        this.level = 1;
    }
    this.updatedAt = LocalDateTime.now();
}
```

However:
1. **OrganizationController** has NO endpoint for updating parent organization
2. **OrgMapper** has no handling for parentOrgId in updateEntityFromRequest()
3. **OrganizationApplicationService** has NO method to update parent organization

**Impact:** The entire organization hierarchy management is broken. While the domain supports parent-child relationships, there is no API to create or modify hierarchical structures. All organizations must remain at level 1 (top-level), making the `getOrganizationTree()` endpoint useless for any real hierarchy.

### H-2. `getOrganizationTree()` Ignores `includeUsers` Parameter (False Advertising)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/application/OrganizationApplicationService.java` (lines 90-94)

**Evidence:**
The `getOrganizationTree()` method accepts an `includeUsers` parameter that is never used. The method signature implies it will include users in the tree structure, but the implementation completely ignores this parameter.

```java
// Line 90-94 in OrganizationApplicationService.java
public List<SysOrg> getOrganizationTree(boolean includeUsers, boolean includeDisabled) {
    List<SysOrg> allOrgs = includeDisabled
            ? organizationRepository.findAll()
            : organizationRepository.findByIsActive(true);
    return buildTree(allOrgs, null); // includeUsers is NEVER used!
}
```

The `buildTree()` method (lines 101-116) also has no logic to load or attach users to organizations.

**Impact:**
- API documentation is misleading
- Clients expecting to retrieve organization trees with users will get empty user collections
- The parameter creates unnecessary confusion and violates the principle of least surprise

---

## MEDIUM SEVERITY ISSUES

### M-1. No Cascade or Containment Check for Parent Organization Deletion/Deactivation

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (no containment check on deactivate/delete)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/application/OrganizationApplicationService.java` (no cascade handling)

**Evidence:**
The system allows deactivating or deleting a parent organization without checking if it has active children. This creates orphaned entities in the tree structure with no way to reattach them.

The `SysOrg` entity has no business rules preventing:
1. Deleting a parent organization while children still exist
2. Deactivating a parent organization while children remain active
3. No cascade behavior defined on the `sys_org` table

**Impact:** This leads to:
- Inconsistent organization trees (active children with inactive/deleted parents)
- Data integrity issues
- Navigation problems in the UI when loading tree structures

### M-2. Duplicate `OrgType` Enums (Bounded Context Inconsistency)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/OrgType.java` (lines 1-66)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-shared-kernel/src/main/java/com/sism/enums/OrgType.java` (exists but not shown in audit)

**Evidence:**
The organization module has its own `OrgType` enum that duplicates the shared-kernel `com.sism.enums.OrgType`. Both have identical values (admin, functional, academic), but they are separate classes in different packages.

```java
// Organization module (local)
package com.sism.organization.domain;
public enum OrgType { admin, functional, academic; ... }

// Shared kernel (cross-module)
package com.sism.enums;
public enum OrgType { admin, functional, academic; ... }
```

**Impact:**
1. **Maintenance overhead:** Changes to org types must be synchronized in two places
2. **Conversion complexity:** Requires constant conversion between the two enum types
3. **Potential bugs:** If values drift out of sync, the system could fail
4. **Code smell:** Violates DRY principle and bounded context consistency

### M-3. Tree Building is O(n²) Time Complexity (Performance Issue)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/application/OrganizationApplicationService.java` (lines 101-116)

**Evidence:**
The `buildTree()` method uses a recursive stream approach with linear filtering for each node, resulting in O(n²) time complexity:

```java
private List<SysOrg> buildTree(List<SysOrg> allOrgs, Long parentId) {
    return allOrgs.stream()
            .filter(org -> {
                Long orgParentId = org.getParentOrgId();
                if (parentId == null) return orgParentId == null;
                return parentId.equals(orgParentId);
            })
            .peek(org -> {
                List<SysOrg> children = buildTree(allOrgs, org.getId()); // Recursive call with full list
                if (org.getChildren() == null) {
                    org.setChildren(new ArrayList<>());
                }
                org.getChildren().clear();
                org.getChildren().addAll(children);
            })
            .collect(Collectors.toList());
}
```

**Impact:** With large organization hierarchies (1000+ nodes), this method will cause significant performance degradation due to repeated traversal of the entire dataset for each node.

---

## LOW SEVERITY ISSUES

### L-1. Unused `isDeleted` Field and Soft Delete (No Business Logic)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (lines 63, 177-180)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/infrastructure/persistence/JpaOrganizationRepositoryInternal.java` (no findByIsDeleted method)

**Evidence:**
The `SysOrg` entity has an `isDeleted` field with a `delete()` method:

```java
@Column(name = "is_deleted", nullable = false)
private Boolean isDeleted = false;

// ...

public void delete() {
    this.isDeleted = true;
    this.updatedAt = LocalDateTime.now();
}
```

However:
1. No repository method for `findByIsDeleted(false)` to exclude soft-deleted orgs
2. No service method to handle soft delete logic
3. No API endpoint for deleting organizations
4. The `getAllOrganizations()` and `getOrganizationTree()` methods return all orgs, including deleted ones (unless includeDisabled filter is used, which only checks isActive)

**Impact:** The soft delete feature is implemented at the entity level but not exposed or enforced in business logic or queries, making it useless.

### L-2. `updateDescription()` Method is a No-Op (Placebo Method)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (lines 144-148)

**Evidence:**
The `updateDescription()` method exists but does nothing except update the `updatedAt` timestamp. The entity doesn't even have a `description` field.

```java
public void updateDescription(String description) {
    // Description is not currently stored in the entity - this method is for future use
    // This is a placeholder to support the test method
    this.updatedAt = LocalDateTime.now();
}
```

**Impact:**
- Confusing for developers (method exists but doesn't work)
- Test method dependency that provides no real value
- Potential bugs if developers start using this method expecting it to store data

### L-3. Duplicate Endpoint: `/api/v1/organizations/departments` (Alias with No Change)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/interfaces/rest/OrganizationController.java` (lines 53-58)

**Evidence:**
```java
@GetMapping("/departments")
@PreAuthorize("hasAnyRole('ADMIN', 'ORG_MANAGER')")
@Operation(summary = "获取所有部门(旧版别名)")
public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllDepartments() {
    return getAllOrganizations();
}
```

This endpoint is a direct alias to `getAllOrganizations()` with no filtering or transformation. The summary says "获取所有部门" (get all departments) but it returns all organizations regardless of type.

**Impact:**
- Misleading API documentation
- Duplicate code that serves no purpose
- Potential confusion for clients about when to use which endpoint

---

## MEDIUM SEVERITY ISSUES (CONTINUED)

### M-4. No Validation for Circular Hierarchies

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (updateParent() method)

**Evidence:**
The `updateParent()` method only checks that an organization cannot be its own direct parent, but it does NOT prevent circular hierarchies:

```java
// Line 156 in SysOrg.java
if (parentOrg != null && parentOrg.id != null && parentOrg.id.equals(this.id)) {
    throw new IllegalArgumentException("Organization cannot be its own parent");
}
```

**Impact:** This allows creating circular parent-child relationships like:
- Org A → Org B → Org C → Org A

When building the organization tree, this would cause an infinite recursion and stack overflow.

### M-5. No Sort Order Validation or Constraints

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (lines 128-134)

**Evidence:**
The `updateSortOrder()` method accepts any integer value:

```java
public void updateSortOrder(Integer sortOrder) {
    if (Objects.isNull(sortOrder) || sortOrder < 0) {
        throw new IllegalArgumentException("Sort order must be a non-negative integer");
    }
    this.sortOrder = sortOrder;
    this.updatedAt = LocalDateTime.now();
}
```

However, there are no constraints on:
1. Duplicate sort order values within the same parent
2. Maximum sort order value
3. Sorting behavior in the `buildTree()` method (no order specified when collecting children)

**Impact:** Sort orders are accepted but not enforced or used consistently, leading to unpredictable tree display in the UI.

---

## LOW SEVERITY ISSUES (CONTINUED)

### L-4. `getOrgCode()` Uses Type Name Instead of Code (Misleading)

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (lines 188-190)

**Evidence:**
The `getOrgCode()` method generates a code that combines type name and ID:

```java
public String getOrgCode() {
    return this.type.name() + "_" + this.id;
}
```

**Impact:** This is misleading because:
1. The name suggests an "organization code" but it's just a type-ID combination
2. No standard format or validation
3. No guarantee of uniqueness (though it will be unique in practice due to ID)

### L-5. Redundant Methods: `getOrgName()` and `setOrgName()`

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (lines 184-198)

**Evidence:**
These methods are redundant with the standard getter/setter:

```java
public String getOrgName() {
    return this.name;
}

public void setOrgName(String orgName) {
    this.name = orgName;
}
```

**Impact:**
- Violates JavaBean conventions (standard is `getName()/setName()`)
- Creates unnecessary code duplication
- Confusing for developers working with the entity

---

## HIGH SEVERITY ISSUES (CONTINUED)

### H-3. No Constraint or Validation on Organization Name Length

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (validate() method)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/interfaces/dto/OrgRequest.java` (no @Size constraint)

**Evidence:**
The database column is defined as `VARCHAR(100)`:

```sql
-- From JPA annotations in SysOrg.java
@Column(name = "name", nullable = false, unique = true, length = 100)
private String name;
```

But there is NO validation in:
1. **OrgRequest.java** - No @Size(min=1, max=100) annotation
2. **SysOrg.java.validate()** - No length check
3. **SysOrg.create()** - No length validation

```java
// SysOrg.java - Line 213 (validate() method)
public void validate() {
    if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Organization name is required");
    }
    // NO CHECK FOR NAME LENGTH!
    if (type == null) {
        throw new IllegalArgumentException("Organization type is required");
    }
    if (level == null || level < 1) {
        throw new IllegalArgumentException("Organization level must be at least 1");
    }
}
```

**Impact:** The API will accept organization names longer than 100 characters, which will fail at the database level with a constraint violation. This creates an unhandled exception that propagates to the client as a 500 error.

---

## MEDIUM SEVERITY ISSUES (CONTINUED)

### M-6. No Audit Log for Organization Changes

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/domain/SysOrg.java` (no @CreatedBy/@LastModifiedBy)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-organization/src/main/java/com/sism/organization/application/OrganizationApplicationService.java` (no audit log calls)

**Evidence:**
The `SysOrg` entity tracks `createdAt` and `updatedAt` timestamps but not the users who made the changes:

```java
@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
```

**Impact:**
- No accountability for who created or modified organizations
- Difficult to trace changes for audit purposes
- No way to enforce segregation of duties through audit trails

---

## Summary of Findings

| Severity | Count | Issues |
|----------|-------|--------|
| HIGH | 3 | No API for parent org, ignored includeUsers, no name length validation |
| MEDIUM | 6 | No cascade on parent deactivation, duplicate enums, O(n²) tree building, no circular hierarchy check, no sort order constraints, no audit log |
| LOW | 5 | Unused isDeleted field, updateDescription no-op, duplicate endpoint, redundant methods, misleading orgCode |

---

## CRITICAL RECOMMENDATIONS

1. **Implement a PUT/PATCH endpoint for updating parent organization** - Fix the broken hierarchy management
2. **Fix the getOrganizationTree() method** - Implement the includeUsers functionality
3. **Add validation for organization name length** - Prevent 500 errors from database constraint violations
4. **Add circular hierarchy detection** - Prevent stack overflow from infinite recursion
5. **Implement cascade behavior for parent deactivation/deletion** - Maintain data integrity

---

## MEDIUM PRIORITY RECOMMENDATIONS

1. **Remove duplicate OrgType enum** - Use only the shared-kernel version
2. **Optimize tree building algorithm** - Reduce from O(n²) to O(n) using hash maps
3. **Add audit logging** - Track who made changes to organizations
4. **Implement proper soft delete logic** - Either enforce it or remove the unused field
5. **Add sort order constraints and validation** - Ensure consistency in tree display

---

## LOW PRIORITY RECOMMENDATIONS

1. **Remove the placebo updateDescription() method** - It serves no real purpose
2. **Remove duplicate /departments endpoint** - Or implement proper filtering for departments
3. **Rename getOrgCode()** - To something more accurate like getTypeAndId()
4. **Remove redundant getOrgName()/setOrgName() methods** - Use standard getName()/setName()

