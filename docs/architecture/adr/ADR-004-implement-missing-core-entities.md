# ADR-004: Implement Missing Core Entities

## Status

Accepted

## Date

2026-02-13

## Context

The codebase audit revealed several missing entities that are essential for complete business functionality:
- **Attachment** - File management (uploads, downloads, metadata)
- **AuditFlowDef** - Audit workflow definitions
- **AuditStepDef** - Individual audit step configurations
- **AuditInstance** - Runtime audit workflow instances
- **WarnLevel** - Warning level configurations for alerts
- **PlanReportIndicator** - Plan-report-indicator relationships
- **PlanReportIndicatorAttachment** - Attachments on report indicators

These entities were referenced in database schema but had no corresponding Java entities, preventing full utilization of the system's capabilities. The database tables already existed in production (V1__baseline_schema.sql), but the application layer couldn't interact with them.

## Decision

We will implement all missing core entities with complete business layer support:
1. Create JPA entity classes matching existing database schemas
2. Implement repositories with custom query methods
3. Create service layer with business logic
4. Implement REST controllers with OpenAPI documentation
5. Create DTOs for requests and VOs for responses
6. Write comprehensive unit tests for each entity

Priority order:
- **High**: Attachment, WarnLevel (immediate business needs)
- **Medium**: AuditFlowDef, AuditStepDef, AuditInstance (flexible workflow support)
- **Low**: PlanReportIndicator, PlanReportIndicatorAttachment (optional enhancements)

## Consequences

### Positive

- Complete business functionality (file management, audit workflows, warnings)
- Consistent architecture across all entities
- Full CRUD operations via REST API
- Proper validation and error handling
- Comprehensive test coverage
- Production-ready implementation
- Enables new features (file uploads, configurable workflows)

### Negative

- Increased codebase size (~2,000 lines of code)
- More entities to maintain
- Additional API endpoints to document
- Learning curve for developers unfamiliar with new entities

### Neutral

- No changes to existing entities or functionality
- Database schema already exists (no migrations needed for most entities)
- Follows existing code patterns (easy to understand)

## Alternatives Considered

### Alternative 1: Implement only high-priority entities

Implement only Attachment and WarnLevel, defer others indefinitely.

**Why not chosen**:
- Leaves system incomplete
- Future implementation requires revisiting architecture decisions
- Audit workflow functionality remains unavailable
- Inconsistent entity coverage

### Alternative 2: Implement entities without full business layer

Create entities and repositories only, skip services and controllers.

**Why not chosen**:
- Inconsistent with existing architecture
- Requires manual SQL queries for operations
- No REST API access
- Poor developer experience
- Violates layered architecture pattern

### Alternative 3: Use generic entity framework

Use a generic CRUD framework (e.g., Spring Data REST) to auto-generate APIs.

**Why not chosen**:
- Less control over business logic
- Generic APIs may not match business requirements
- Inconsistent with existing controller patterns
- Harder to add custom validation and error handling

## Implementation Notes

**Entity Implementation Pattern**:
```java
@Entity
@Table(name = "attachment")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Size(max = 255)
    private String originalName;
    
    // ... other fields with validation
}
```

**Service Pattern**:
```java
@Service
@Transactional
public class AttachmentService {
    private final AttachmentRepository repository;
    
    public AttachmentService(AttachmentRepository repository) {
        this.repository = repository;
    }
    
    public AttachmentVO upload(AttachmentUploadRequest request) {
        // Business logic
    }
}
```

**Controller Pattern**:
```java
@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachment Management")
public class AttachmentController {
    private final AttachmentService service;
    
    @PostMapping("/upload")
    @Operation(summary = "Upload file")
    public ApiResponse<AttachmentVO> upload(@Valid @RequestBody AttachmentUploadRequest request) {
        return ApiResponse.success(service.upload(request));
    }
}
```

**Execution Results**:
- Entities created: 5 (Attachment, AuditFlowDef, AuditStepDef, AuditInstance, WarnLevel)
- Repositories created: 5
- Services created: 4 (AuditInstance deferred)
- Controllers created: 4
- DTOs/VOs created: 13
- Unit tests created: 150+
- Total lines of code: ~2,000

## References

- Task 2.1: Attachment Entity Implementation
- Task 2.2-2.4: Complete Business Layer for New Entities
- Entity Inventory: `sism-backend/docs/audit/entity-inventory.md`
- Service Inventory: `sism-backend/docs/audit/service-inventory.md`
