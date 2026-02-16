# ADR-003: Defer Entity Renaming

## Status

Accepted

## Date

2026-02-13

## Context

The codebase has several entity naming inconsistencies where Java class names don't match database table names:
- `AssessmentCycle` entity → `cycle` table (should be `Cycle`)
- `StrategicTask` entity → `task` table (should be `Task`)
- `Milestone` entity → `indicator_milestone` table (should be `IndicatorMilestone`)

While these inconsistencies violate naming conventions and can confuse developers, the entities are:
- Currently in production and stable
- Used extensively throughout the codebase (repositories, services, controllers, DTOs, VOs)
- Working correctly with @Table annotations mapping to correct database tables

Renaming would require:
- Updating 50+ files across multiple layers
- Comprehensive testing to ensure no references are missed
- Risk of introducing bugs in a stable system
- Estimated 8-12 hours of work

## Decision

We will **defer entity renaming** to a future phase and maintain the current entity names:
1. Keep `AssessmentCycle`, `StrategicTask`, and `Milestone` as-is
2. Document the naming inconsistency in code comments
3. Use @Table annotations to maintain correct database mappings
4. Focus refactoring efforts on higher-value, lower-risk improvements
5. Revisit entity renaming only if:
   - Team has bandwidth for comprehensive testing
   - Business value justifies the effort
   - Risk tolerance increases

This follows the principle: "If it ain't broke, don't fix it."

## Consequences

### Positive

- Zero risk of introducing bugs in stable production code
- Team can focus on higher-value improvements (new features, missing entities)
- No breaking changes for existing code
- Faster delivery of refactoring project
- Maintains system stability

### Negative

- Naming inconsistency remains in codebase
- New developers may be confused by entity vs. table name mismatch
- Technical debt persists
- Future renaming becomes harder as codebase grows

### Neutral

- @Table annotations already handle the mapping correctly
- No runtime performance impact
- Database schema remains unchanged

## Alternatives Considered

### Alternative 1: Rename entities immediately

Perform comprehensive entity renaming as part of current refactoring.

**Why not chosen**:
- High risk of introducing bugs (50+ files to update)
- Requires extensive testing (8-12 hours)
- Low business value compared to implementing missing features
- Current naming works correctly with @Table annotations
- Team prioritizes stability over perfect naming

### Alternative 2: Rename entities incrementally

Rename one entity at a time over multiple sprints.

**Why not chosen**:
- Still carries risk of bugs for each rename
- Prolongs the inconsistency period
- Requires multiple rounds of testing
- Effort better spent on new features

### Alternative 3: Create new entities with correct names

Create new entities (Cycle, Task, IndicatorMilestone) alongside old ones, then migrate.

**Why not chosen**:
- Creates temporary duplication and confusion
- Requires maintaining two parallel implementations
- More complex than direct renaming
- Higher total effort than Alternative 1

## Implementation Notes

**Documentation Added**:
```java
/**
 * Assessment Cycle entity.
 * 
 * Note: Entity name is AssessmentCycle but maps to 'cycle' table.
 * This naming inconsistency is intentional to maintain backward compatibility.
 * See ADR-003 for rationale.
 * 
 * @see com.sism.repository.AssessmentCycleRepository
 */
@Entity
@Table(name = "cycle")
public class AssessmentCycle extends BaseEntity {
    // ...
}
```

**Future Considerations**:
If entity renaming is pursued in the future, use IDE refactoring tools:
1. IntelliJ IDEA: Refactor → Rename (Shift+F6)
2. Ensure "Search in comments and strings" is enabled
3. Run full test suite after each rename
4. Use git branches for isolation

## References

- Design Document: Migration Strategy - Phase 2 (Entity Naming Standardization)
- Risk Assessment Matrix: Phase 2 marked as 🟡 Medium risk
- Task 1.2: Codebase Baseline Documentation
