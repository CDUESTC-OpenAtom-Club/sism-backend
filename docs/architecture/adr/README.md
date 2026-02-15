# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) for the SISM Backend Architecture Refactoring project.

## What is an ADR?

An Architecture Decision Record (ADR) is a document that captures an important architectural decision made along with its context and consequences.

## ADR Format

Each ADR follows this structure:
- **Title**: Short noun phrase describing the decision
- **Status**: Proposed, Accepted, Deprecated, or Superseded
- **Context**: The issue motivating this decision
- **Decision**: The change we're proposing or have agreed to
- **Consequences**: The resulting context after applying the decision

## Index of ADRs

### Phase 1: Foundation Cleanup
- [ADR-001: Remove Deprecated Entities](./ADR-001-remove-deprecated-entities.md) - Accepted
- [ADR-002: Preserve Dual Repository Pattern](./ADR-002-preserve-dual-repository-pattern.md) - Accepted
- [ADR-003: Defer Entity Renaming](./ADR-003-defer-entity-renaming.md) - Accepted

### Phase 2: New Entity Implementation
- [ADR-004: Implement Missing Core Entities](./ADR-004-implement-missing-core-entities.md) - Accepted
- [ADR-005: Use Flat Package Structure](./ADR-005-use-flat-package-structure.md) - Accepted
- [ADR-006: Soft Delete Pattern for Attachments](./ADR-006-soft-delete-pattern-attachments.md) - Accepted

### Phase 3: Database Migration
- [ADR-007: Adopt Flyway for Schema Management](./ADR-007-adopt-flyway-schema-management.md) - Accepted
- [ADR-008: Use Idempotent Migration Scripts](./ADR-008-use-idempotent-migration-scripts.md) - Accepted

### Phase 4: Testing Strategy
- [ADR-009: Use H2 for Unit Tests](./ADR-009-use-h2-for-unit-tests.md) - Accepted
- [ADR-010: Defer TestContainers Integration](./ADR-010-defer-testcontainers-integration.md) - Accepted

### Phase 5: Risk Management
- [ADR-011: Phased Migration Approach](./ADR-011-phased-migration-approach.md) - Accepted
- [ADR-012: Defer DDD Package Restructure](./ADR-012-defer-ddd-package-restructure.md) - Accepted

## Decision Status

- **Accepted**: 12 decisions
- **Proposed**: 0 decisions
- **Deprecated**: 0 decisions
- **Superseded**: 0 decisions

## How to Create a New ADR

1. Copy the template from `ADR-template.md`
2. Number it sequentially (ADR-XXX)
3. Fill in all sections with relevant information
4. Update this README with the new ADR
5. Commit the ADR with the implementation

## References

- [ADR GitHub Organization](https://adr.github.io/)
- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
