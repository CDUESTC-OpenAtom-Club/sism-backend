package com.sism.repository;

import com.sism.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository interface for Attachment entity
 * Provides data access methods for file attachment management
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * Find all attachments uploaded by a specific user
     */
    List<Attachment> findByUploadedBy(Long userId);

    /**
     * Find attachments by content type
     */
    List<Attachment> findByContentType(String contentType);

    /**
     * Find attachments by original file name (partial match)
     */
    @Query("SELECT a FROM Attachment a WHERE a.originalName LIKE %:originalName%")
    List<Attachment> findByOriginalNameContaining(@Param("originalName") String originalName);

    /**
     * Find attachments uploaded within a date range
     */
    @Query("SELECT a FROM Attachment a WHERE a.uploadedAt BETWEEN :startDate AND :endDate")
    List<Attachment> findByUploadedAtBetween(@Param("startDate") OffsetDateTime startDate, 
                                               @Param("endDate") OffsetDateTime endDate);

    /**
     * Find attachments larger than specified size
     */
    @Query("SELECT a FROM Attachment a WHERE a.sizeBytes > :minSize")
    List<Attachment> findBySizeBytesGreaterThan(@Param("minSize") Long minSize);

    /**
     * Find attachments by uploader and date range
     */
    @Query("SELECT a FROM Attachment a WHERE a.uploadedBy = :userId " +
           "AND a.uploadedAt BETWEEN :startDate AND :endDate")
    List<Attachment> findByUploaderAndDateRange(@Param("userId") Long userId,
                                                  @Param("startDate") OffsetDateTime startDate,
                                                  @Param("endDate") OffsetDateTime endDate);

    /**
     * Count attachments by uploader
     */
    long countByUploadedBy(Long userId);

    /**
     * Calculate total file size by uploader
     */
    @Query("SELECT SUM(a.sizeBytes) FROM Attachment a WHERE a.uploadedBy = :userId")
    Long sumFileSizeByUploader(@Param("userId") Long userId);

    /**
     * Find recent attachments (last N days)
     */
    @Query("SELECT a FROM Attachment a WHERE a.uploadedAt >= :sinceDate ORDER BY a.uploadedAt DESC")
    List<Attachment> findRecentAttachments(@Param("sinceDate") OffsetDateTime sinceDate);

    /**
     * Find attachments by content type category (e.g., "image/", "application/pdf")
     */
    @Query("SELECT a FROM Attachment a WHERE a.contentType LIKE :contentTypePattern%")
    List<Attachment> findByContentTypeStartingWith(@Param("contentTypePattern") String contentTypePattern);

    /**
     * Find attachments by storage driver
     */
    List<Attachment> findByStorageDriver(String storageDriver);

    /**
     * Find attachments by bucket
     */
    List<Attachment> findByBucket(String bucket);
}
