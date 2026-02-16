# ADR-012: Defer DDD Package Restructure

## Status

Accepted

## Date

2026-02-13

## Context

The original design document proposed a comprehensive Domain-Driven Design (DDD) package restructure:

**Current Structure** (Flat):
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

**Proposed Structure** (DDD Layers):
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

Analysis of the restructure:
- **Scope**: 180+ source files need to be moved
- **Effort**: Estimated 16-20 hours
- **Risk**: 🔴 High (global impact, many import changes)
- **Value**: Theoretical architectural purity
- **Current State**: Flat structure works well, team is productive
- **Team Familiarity**: Team comfortable with current structure
- **Project Size**: ~180 files (small-to-medium, not requiring DDD complexity)

The restructure would require:
- Moving all 180+ files to new packages
- Updating thousands of import statements
- Comprehensive testing to catch missed references
- Team training on DDD conventions
- Updating all documentation
- Risk of breaking existing functionality

## Decision

We will **defer DDD package restructure indefinitely** and maintain the flat package structure:
1. Keep current flat organization (entity, repository, service, controller, dto, vo)
2. Enforce layering through code review and conventions
3. Document package responsibilities clearly
4. Consider DDD structure only if:
   - Project grows beyond 500 source files
   - Team explicitly requests it
   - Clear business value emerges
   - Risk tolerance significantly increases

This follows the principles:
- "Optimize for team productivity over theoretical purity"
- "If it ain't broke, don't fix it"
- "YAGNI (You Aren't Gonna Need It)"

## Consequences

### Positive

- Zero migration risk (no files moved)
- Team maintains full productivity
- Simple, intuitive package names
- Easy onboarding for new developers
- Faster refactoring project completion
- Stable production system
- Focus on business value over structure

### Negative

- Doesn't follow DDD package conventions
- Layering enforced by convention, not structure
- May need restructure if project grows significantly
- Technical debt for "perfect" architecture
- Some developers may prefer DDD structure

### Neutral

- Layered architecture still enforced through code review
- No runtime performance impact
- Spring component scanning works identically
- Can be implemented later if truly needed

## Alternatives Considered

### Alternative 1: Full DDD package restructure now

Implement complete DDD package structure as designed.

**Why not chosen**:
- Very high risk (180+ files, thousands of imports)
- Very high effort (16-20 hours)
- Low immediate business value
- Team unfamiliar with DDD conventions
- Current structure works well
- Project size doesn't justify complexity
- Risk of breaking production functionality

### Alternative 2: Hybrid approach

Add domain/application/interfaces as top-level packages, keep subpackages flat.

**Why not chosen**:
- Still requires moving all files
- Adds complexity without clear benefit
- Confusing mix of conventions
- Doesn't reduce migration risk
- No clear advantage over full DDD or flat structure

### Alternative 3: Gradual migration over time

Move packages incrementally over multiple months.

**Why not chosen**:
- Prolonged inconsistency (confusing for developers)
- Multiple rounds of testing required
- Higher total effort than one-time migration
- Disrupts team productivity repeatedly
- No clear trigger for when to migrate each package

### Alternative 4: Create new modules with DDD structure

Keep existing code flat, use DDD for new modules.

**Why not chosen**:
- Creates inconsistency in codebase
- Confusing for developers (which convention to follow?)
- Doesn't solve the "problem" (if it exists)
- Adds complexity without clear benefit

## Implementation Notes

**Package Responsibilities** (documented in README):
```
com.sism/
├── entity/          # JPA entities (domain models)
│                    # Responsibility: Data structure, validation, relationships
│
├── repository/      # Data access layer
│                    # Responsibility: Database operations, custom queries
│
├── service/         # Business logic layer
│                    # Responsibility: Business rules, transactions, orchestration
│
├── controller/      # REST API layer
│                    # Responsibility: HTTP handling, request/response mapping
│
├── dto/             # Request objects
│                    # Responsibility: API input validation, deserialization
│
├── vo/              # Response objects
│                    # Responsibility: API output formatting, serialization
│
├── config/          # Spring configuration
│                    # Responsibility: Bean definitions, app configuration
│
├── exception/       # Exception handling
│                    # Responsibility: Error handling, exception mapping
│
├── util/            # Utility classes
│                    # Responsibility: Helper functions, common operations
│
├── common/          # Common classes
│                    # Responsibility: Shared models (ApiResponse, etc.)
│
└── enums/           # Enumeration types
                     # Responsibility: Type-safe constants, domain values
```

**Layering Rules** (enforced by code review):
1. **Controllers** depend on Services (not Repositories directly)
2. **Services** depend on Repositories (not Controllers)
3. **Entities** have no dependencies on other layers
4. **DTOs/VOs** are used at API boundaries (never expose entities)
5. **Repositories** only accessed by Services
6. **Utilities** have no business logic

**Code Review Checklist**:
- [ ] Controllers don't inject Repositories directly
- [ ] Services don't return Entities (use VOs)
- [ ] Controllers don't contain business logic
- [ ] Entities don't reference Services or Controllers
- [ ] DTOs/VOs don't contain business logic

**Future Considerations**:
If project grows beyond 500 source files, revisit DDD structure:
- Evaluate actual pain points with flat structure
- Assess team's DDD knowledge and interest
- Use automated refactoring tools (IntelliJ IDEA)
- Migrate one module at a time
- Maintain backward compatibility during transition
- Comprehensive testing at each step

**Triggers for Reconsideration**:
- Project exceeds 500 source files
- Team struggles with layering violations
- New team members consistently confused by structure
- Business explicitly requests DDD structure
- Microservices architecture adopted (each service could use DDD)

## References

- Design Document: Migration Strategy - Phase 6 (DDD Package Restructure)
- Risk Assessment Matrix: Phase 6 marked as 🔴 High risk
- ADR-005: Use Flat Package Structure
- ADR-011: Phased Migration Approach
- Martin Fowler on DDD: https://martinfowler.com/bliki/DomainDrivenDesign.html
