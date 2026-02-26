# ADR-005: Use Flat Package Structure

## Status

Accepted

## Date

2026-02-13

## Context

The original design document proposed reorganizing the codebase into a Domain-Driven Design (DDD) layered structure:
```
com.sism/
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/
├── application/
│   └── service/
└── interfaces/
    ├── rest/
    ├── dto/
    └── vo/
```

However, the current codebase uses a flat package structure:
```
com.sism/
├── entity/
├── repository/
├── service/
├── controller/
├── dto/
├── vo/
├── config/
├── exception/
└── util/
```

The flat structure:
- Is already in production and stable
- Is familiar to the entire development team
- Works well for the current project size (~180 source files)
- Requires no migration effort
- Has clear, simple organization

Migrating to DDD structure would:
- Require updating 180+ files
- Risk introducing bugs through incorrect package moves
- Require updating all import statements
- Need comprehensive testing
- Estimated 16-20 hours of work
- Provide unclear benefits for current project size

## Decision

We will **maintain the flat package structure** and defer DDD package reorganization:
1. Keep current package organization (entity, repository, service, controller, dto, vo)
2. Document package responsibilities in README
3. Enforce layering through code review rather than package structure
4. Consider DDD structure only if:
   - Project grows significantly (>500 source files)
   - Team explicitly requests it
   - Clear business value emerges

This follows the principle: "Optimize for readability and team familiarity over theoretical purity."

## Consequences

### Positive

- Zero migration risk (no files moved)
- Team maintains productivity (no learning curve)
- Simple, clear package names
- Easy navigation for new developers
- Faster refactoring completion
- Stable production system

### Negative

- Doesn't follow DDD package conventions
- Layering enforced by convention, not structure
- May need reorganization if project grows significantly
- Technical debt for "perfect" architecture

### Neutral

- Layered architecture still enforced through code review
- No runtime performance impact
- Spring component scanning works identically

## Alternatives Considered

### Alternative 1: Full DDD package restructure

Implement complete DDD package structure as designed.

**Why not chosen**:
- High risk (180+ files to move)
- High effort (16-20 hours)
- Low immediate value
- Team unfamiliar with DDD conventions
- Current structure works well
- Risk of breaking imports and dependencies

### Alternative 2: Hybrid approach

Keep flat structure but add domain/application/interfaces as top-level packages.

**Why not chosen**:
- Still requires moving all files
- Adds complexity without clear benefit
- Confusing mix of old and new conventions
- Doesn't solve the migration risk

### Alternative 3: Gradual migration

Move packages incrementally over multiple sprints.

**Why not chosen**:
- Prolonged inconsistency period
- Multiple rounds of testing required
- Confusing for developers during transition
- Higher total effort than one-time migration

## Implementation Notes

**Package Responsibilities** (documented in README):

```
com.sism/
├── entity/          # JPA entities (domain models)
├── repository/      # Data access layer
├── service/         # Business logic layer
├── controller/      # REST API layer
├── dto/             # Request objects
├── vo/              # Response objects
├── config/          # Spring configuration
├── exception/       # Exception handling
├── util/            # Utility classes
├── common/          # Common classes (ApiResponse, etc.)
└── enums/           # Enumeration types
```

**Layering Rules** (enforced by code review):
1. Controllers depend on Services (not Repositories)
2. Services depend on Repositories (not Controllers)
3. Entities have no dependencies on other layers
4. DTOs/VOs are used for API boundaries (never expose entities)

**Future Considerations**:
If project grows beyond 500 source files, revisit DDD structure:
- Use automated refactoring tools
- Migrate one module at a time
- Maintain backward compatibility during transition

## References

- Design Document: Migration Strategy - Phase 6 (DDD Package Restructure)
- Risk Assessment Matrix: Phase 6 marked as 🔴 High risk
- ADR-012: Defer DDD Package Restructure
