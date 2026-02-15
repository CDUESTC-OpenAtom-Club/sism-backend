# ADR-002: Preserve Dual Repository Pattern

## Status

Accepted

## Date

2026-02-13

## Context

During the codebase audit, we discovered two repository interfaces for user management:
- `UserRepository` - Extended interface with 7+ custom query methods
- `SysUserRepository` - Basic JPA repository with standard CRUD operations

Initial refactoring plans suggested consolidating to a single repository to eliminate duplication. However, investigation revealed both repositories serve different purposes and are actively used:
- `UserRepository` is used by 7 classes requiring custom queries (findByUsername, findByRole, etc.)
- `SysUserRepository` is used by services needing only basic CRUD operations
- Both map to the same `sys_user` database table

## Decision

We will preserve both `UserRepository` and `SysUserRepository` as they serve different architectural purposes:
1. Keep `UserRepository` for complex query operations
2. Keep `SysUserRepository` for simple CRUD operations
3. Document the coexistence pattern in code comments
4. Ensure both repositories remain synchronized with the `SysUser` entity

This follows the Interface Segregation Principle - clients should not be forced to depend on interfaces they don't use.

## Consequences

### Positive

- No breaking changes to existing code (7+ classes continue working)
- Clear separation of concerns (complex queries vs. simple CRUD)
- Follows Interface Segregation Principle
- Zero risk of introducing bugs during consolidation
- Services can choose the appropriate repository based on their needs

### Negative

- Slight duplication in repository layer
- Developers must understand when to use which repository
- Potential confusion for new team members

### Neutral

- Both repositories map to the same entity and table
- No performance impact (both use JPA under the hood)

## Alternatives Considered

### Alternative 1: Consolidate to single UserRepository

Merge both repositories into a single `UserRepository` with all methods.

**Why not chosen**: 
- Would require updating 7+ classes that use `SysUserRepository`
- Violates Interface Segregation Principle (forces all clients to see all methods)
- Higher risk of introducing bugs during migration
- No clear benefit over current pattern

### Alternative 2: Consolidate to SysUserRepository

Keep only `SysUserRepository` and add custom query methods to it.

**Why not chosen**:
- Would require renaming in 7+ classes using `UserRepository`
- Custom queries would pollute the "Sys" naming convention
- Higher refactoring effort with minimal benefit

### Alternative 3: Create UserRepositoryFacade

Create a facade that delegates to both repositories.

**Why not chosen**:
- Adds unnecessary complexity
- No clear benefit over direct repository usage
- Would still require both underlying repositories

## Implementation Notes

**Documentation Added**:
```java
/**
 * Extended repository for User entity with custom query methods.
 * Use this repository when you need complex queries (findByUsername, findByRole, etc.).
 * For simple CRUD operations, consider using SysUserRepository.
 * 
 * @see SysUserRepository for basic CRUD operations
 */
public interface UserRepository extends JpaRepository<SysUser, Long> {
    // Custom query methods...
}

/**
 * Basic repository for User entity with standard CRUD operations.
 * Use this repository for simple CRUD operations.
 * For complex queries, use UserRepository.
 * 
 * @see UserRepository for custom query methods
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    // Standard CRUD only
}
```

**Usage Pattern**:
- Use `UserRepository` when you need: findByUsername, findByRole, custom queries
- Use `SysUserRepository` when you need: save, findById, delete, findAll

## References

- Task 1.1: Deprecated Code Removal (verification phase)
- Code Review Report: `sism-backend/docs/audit/task-1.1-code-review.md`
- Repository Inventory: `sism-backend/docs/audit/repository-inventory.md`
