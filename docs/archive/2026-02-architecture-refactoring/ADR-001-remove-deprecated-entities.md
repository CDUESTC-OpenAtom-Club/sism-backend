# ADR-001: Remove Deprecated Entities

## Status

Accepted

## Date

2026-02-13

## Context

The codebase contained deprecated entity files that were marked for removal but still present in the repository:
- `Org.java.deprecated` - A deprecated organization entity that was replaced by `SysOrg`
- The deprecated file was causing confusion for developers
- Database migrations (V1.5 and V1.7) had already migrated data from `org` to `sys_org` table
- No active code references existed to the deprecated entity

The presence of deprecated code increases maintenance burden and can lead to accidental usage by developers unfamiliar with the migration history.

## Decision

We will immediately remove all deprecated entity files from the codebase, specifically:
1. Delete `sism-backend/src/main/java/com/sism/entity/Org.java.deprecated`
2. Verify no compilation errors after removal
3. Confirm no active references exist through grep search
4. Document the removal in git commit history

## Consequences

### Positive

- Cleaner codebase with no ambiguity about which entities to use
- Reduced maintenance burden
- Prevents accidental usage of deprecated entities
- Faster code navigation and IDE indexing
- Clear signal to developers that `SysOrg` is the canonical entity

### Negative

- Loss of historical reference in the codebase (mitigated by git history)
- Developers cannot easily see what the old entity looked like (mitigated by git blame)

### Neutral

- Zero impact on runtime behavior (deprecated code was not in use)
- No database changes required

## Alternatives Considered

### Alternative 1: Keep deprecated files with clear warnings

Keep the deprecated files but add prominent warnings in comments and @Deprecated annotations.

**Why not chosen**: The files were already marked as `.deprecated` in filename, yet still caused confusion. Keeping them provides no value since git history preserves the information.

### Alternative 2: Move to archive directory

Move deprecated files to an `archive/` directory within the source tree.

**Why not chosen**: This adds complexity to the source structure and still requires developers to understand the archive convention. Git history is a better archive mechanism.

## Implementation Notes

**Verification Steps**:
```bash
# Verify no references exist
grep -r "entity\.Org" --include="*.java" src/
# Should return nothing

# Verify compilation succeeds
mvn clean compile
# Should complete with BUILD SUCCESS
```

**Execution Results**:
- File deleted: `sism-backend/src/main/java/com/sism/entity/Org.java.deprecated`
- Compilation: SUCCESS
- References found: 0
- Git commit: 614b057

## References

- Task 1.1: Deprecated Code Removal
- Code Review Report: `sism-backend/docs/audit/task-1.1-code-review.md`
- Database Migration: V1.5 (org to sys_org migration)
