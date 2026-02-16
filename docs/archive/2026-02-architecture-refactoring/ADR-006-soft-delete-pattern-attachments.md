# ADR-006: Soft Delete Pattern for Attachments

## Status

Accepted

## Date

2026-02-13

## Context

The Attachment entity manages file uploads and metadata for the SISM system. When implementing the delete functionality, we needed to decide between:
- **Hard delete**: Permanently remove records from the database
- **Soft delete**: Mark records as deleted but retain data

Considerations:
- Attachments may be referenced by multiple entities (reports, indicators, tasks)
- Audit trail requirements mandate tracking who deleted what and when
- Compliance requirements may require retaining file metadata even after "deletion"
- Users may accidentally delete files and need recovery
- Storage costs for retaining deleted file metadata are minimal

The database schema already includes soft delete columns:
- `is_deleted` (BOOLEAN)
- `deleted_at` (TIMESTAMP)

## Decision

We will implement **soft delete pattern** for the Attachment entity:
1. Add `isDeleted` and `deletedAt` fields to Attachment entity
2. Implement `softDelete()` method that sets `isDeleted = true` and `deletedAt = now()`
3. Add repository query methods that filter out deleted records by default
4. Provide admin endpoints to view deleted attachments
5. Implement hard delete as a separate admin-only operation (future)

Soft delete behavior:
- Default queries exclude deleted records (`WHERE is_deleted = false`)
- Deleted files remain in database for audit trail
- Physical file storage cleanup is handled separately
- Admin users can view deleted attachments for recovery

## Consequences

### Positive

- Complete audit trail (who deleted what and when)
- Accidental deletion recovery possible
- Compliance with data retention requirements
- References to deleted attachments don't break
- Minimal storage overhead (metadata only)
- Safer than hard delete (reversible)

### Negative

- Database grows over time (deleted records accumulate)
- Queries must explicitly filter deleted records
- Slightly more complex query logic
- Need separate process for physical file cleanup
- Need admin tools to manage deleted records

### Neutral

- Physical files may be deleted immediately or retained separately
- Soft delete is transparent to most API consumers
- Performance impact negligible (indexed is_deleted column)

## Alternatives Considered

### Alternative 1: Hard delete

Permanently delete attachment records from database.

**Why not chosen**:
- Loses audit trail
- No recovery from accidental deletion
- May break references from other entities
- Doesn't meet compliance requirements
- Irreversible operation

### Alternative 2: Archive table

Move deleted records to separate `attachment_archive` table.

**Why not chosen**:
- More complex implementation (two tables to manage)
- Requires data migration on delete
- Harder to query across active and archived records
- No significant benefit over soft delete
- More database maintenance overhead

### Alternative 3: Versioning system

Implement full versioning with soft delete as one version state.

**Why not chosen**:
- Over-engineered for current requirements
- Significantly more complex implementation
- Higher storage costs
- Not needed for attachment use case
- Can be added later if needed

## Implementation Notes

**Entity Implementation**:
```java
@Entity
@Table(name = "attachment")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ... other fields
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = OffsetDateTime.now();
    }
}
```

**Repository Queries**:
```java
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    // Default: exclude deleted
    @Query("SELECT a FROM Attachment a WHERE a.isDeleted = false")
    List<Attachment> findAll();
    
    // Explicit: include deleted
    @Query("SELECT a FROM Attachment a WHERE a.isDeleted = true")
    List<Attachment> findDeleted();
    
    // Find by ID (exclude deleted)
    @Query("SELECT a FROM Attachment a WHERE a.id = :id AND a.isDeleted = false")
    Optional<Attachment> findById(@Param("id") Long id);
}
```

**Service Implementation**:
```java
@Service
@Transactional
public class AttachmentService {
    public void delete(Long id) {
        Attachment attachment = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        
        attachment.softDelete();
        repository.save(attachment);
        
        // Optional: Schedule physical file deletion
        fileStorageService.scheduleDelete(attachment.getObjectKey());
    }
}
```

**Future Enhancements**:
- Admin endpoint to permanently delete old records (hard delete)
- Scheduled job to clean up deleted records older than X days
- Recovery endpoint for undeleting attachments
- Audit log integration for delete operations

## References

- Task 2.1: Attachment Entity Implementation
- Database Schema: V1__baseline_schema.sql (attachment table)
- Audit Requirements: SISM Product Overview (Audit Logging feature)
