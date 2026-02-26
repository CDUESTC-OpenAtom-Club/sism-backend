package com.sism.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Attachment entity
 * Tests entity creation and validation constraints
 * 
 * Requirements: Task 2.1 - Attachment entity validation
 */
@DisplayName("Attachment Entity Tests")
class AttachmentEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Helper method to create a valid Attachment entity
     */
    private Attachment createValidAttachment() {
        Attachment attachment = new Attachment();
        attachment.setStorageDriver("FILE");
        attachment.setObjectKey("/uploads/2024/test-file.pdf");
        attachment.setOriginalName("test-file.pdf");
        attachment.setContentType("application/pdf");
        attachment.setFileExt("pdf");
        attachment.setSizeBytes(1024L);
        attachment.setUploadedBy(1L);
        attachment.setUploadedAt(OffsetDateTime.now());
        attachment.setIsDeleted(false);
        return attachment;
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {

        @Test
        @DisplayName("Should create attachment with all required fields")
        void shouldCreateAttachmentWithRequiredFields() {
            // Given
            Attachment attachment = createValidAttachment();

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
            assertThat(attachment.getStorageDriver()).isEqualTo("FILE");
            assertThat(attachment.getObjectKey()).isEqualTo("/uploads/2024/test-file.pdf");
            assertThat(attachment.getOriginalName()).isEqualTo("test-file.pdf");
            assertThat(attachment.getSizeBytes()).isEqualTo(1024L);
            assertThat(attachment.getUploadedBy()).isEqualTo(1L);
            assertThat(attachment.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should create attachment with optional fields")
        void shouldCreateAttachmentWithOptionalFields() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setBucket("my-bucket");
            attachment.setPublicUrl("https://example.com/files/test.pdf");
            attachment.setSha256("a".repeat(64));
            attachment.setEtag("etag-12345");
            attachment.setRemark("Test attachment");

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
            assertThat(attachment.getBucket()).isEqualTo("my-bucket");
            assertThat(attachment.getPublicUrl()).isEqualTo("https://example.com/files/test.pdf");
            assertThat(attachment.getSha256()).hasSize(64);
            assertThat(attachment.getEtag()).isEqualTo("etag-12345");
            assertThat(attachment.getRemark()).isEqualTo("Test attachment");
        }

        @Test
        @DisplayName("Should set default values correctly")
        void shouldSetDefaultValues() {
            // Given/When
            Attachment attachment = new Attachment();

            // Then
            assertThat(attachment.getStorageDriver()).isEqualTo("FILE");
            assertThat(attachment.getIsDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Storage Driver Validation Tests")
    class StorageDriverValidationTests {

        @Test
        @DisplayName("Should reject null storage driver")
        void shouldRejectNullStorageDriver() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setStorageDriver(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Storage driver cannot be blank");
        }

        @Test
        @DisplayName("Should reject blank storage driver")
        void shouldRejectBlankStorageDriver() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setStorageDriver("");

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Storage driver cannot be blank");
        }

        @Test
        @DisplayName("Should reject storage driver exceeding max length")
        void shouldRejectStorageDriverExceedingMaxLength() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setStorageDriver("A".repeat(17)); // Max is 16

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Storage driver must not exceed 16 characters");
        }

        @Test
        @DisplayName("Should accept valid storage driver values")
        void shouldAcceptValidStorageDriverValues() {
            // Given
            String[] validDrivers = {"FILE", "S3", "OSS", "MINIO"};

            for (String driver : validDrivers) {
                Attachment attachment = createValidAttachment();
                attachment.setStorageDriver(driver);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Object Key Validation Tests")
    class ObjectKeyValidationTests {

        @Test
        @DisplayName("Should reject null object key")
        void shouldRejectNullObjectKey() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setObjectKey(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Object key cannot be blank");
        }

        @Test
        @DisplayName("Should reject blank object key")
        void shouldRejectBlankObjectKey() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setObjectKey("");

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Object key cannot be blank");
        }

        @Test
        @DisplayName("Should accept valid object key paths")
        void shouldAcceptValidObjectKeyPaths() {
            // Given
            String[] validKeys = {
                "/uploads/2024/file.pdf",
                "documents/report.docx",
                "images/photo.jpg",
                "data/2024-01-01/export.csv"
            };

            for (String key : validKeys) {
                Attachment attachment = createValidAttachment();
                attachment.setObjectKey(key);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Original Name Validation Tests")
    class OriginalNameValidationTests {

        @Test
        @DisplayName("Should reject null original name")
        void shouldRejectNullOriginalName() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setOriginalName(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Original name cannot be blank");
        }

        @Test
        @DisplayName("Should reject blank original name")
        void shouldRejectBlankOriginalName() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setOriginalName("");

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Original name cannot be blank");
        }

        @Test
        @DisplayName("Should accept various file name formats")
        void shouldAcceptVariousFileNameFormats() {
            // Given
            String[] validNames = {
                "document.pdf",
                "report-2024.xlsx",
                "photo_001.jpg",
                "data.backup.tar.gz",
                "文件名.docx" // Chinese characters
            };

            for (String name : validNames) {
                Attachment attachment = createValidAttachment();
                attachment.setOriginalName(name);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("File Size Validation Tests")
    class FileSizeValidationTests {

        @Test
        @DisplayName("Should reject null file size")
        void shouldRejectNullFileSize() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setSizeBytes(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("File size cannot be null");
        }

        @Test
        @DisplayName("Should reject zero file size")
        void shouldRejectZeroFileSize() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setSizeBytes(0L);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("File size must be positive");
        }

        @Test
        @DisplayName("Should reject negative file size")
        void shouldRejectNegativeFileSize() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setSizeBytes(-100L);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("File size must be positive");
        }

        @Test
        @DisplayName("Should accept valid file sizes")
        void shouldAcceptValidFileSizes() {
            // Given
            Long[] validSizes = {1L, 1024L, 1048576L, 10485760L}; // 1B, 1KB, 1MB, 10MB

            for (Long size : validSizes) {
                Attachment attachment = createValidAttachment();
                attachment.setSizeBytes(size);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Content Type Validation Tests")
    class ContentTypeValidationTests {

        @Test
        @DisplayName("Should accept null content type")
        void shouldAcceptNullContentType() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setContentType(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject content type exceeding max length")
        void shouldRejectContentTypeExceedingMaxLength() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setContentType("A".repeat(129)); // Max is 128

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Content type must not exceed 128 characters");
        }

        @Test
        @DisplayName("Should accept valid MIME types")
        void shouldAcceptValidMimeTypes() {
            // Given
            String[] validMimeTypes = {
                "application/pdf",
                "image/jpeg",
                "image/png",
                "text/plain",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };

            for (String mimeType : validMimeTypes) {
                Attachment attachment = createValidAttachment();
                attachment.setContentType(mimeType);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("File Extension Validation Tests")
    class FileExtensionValidationTests {

        @Test
        @DisplayName("Should accept null file extension")
        void shouldAcceptNullFileExtension() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setFileExt(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject file extension exceeding max length")
        void shouldRejectFileExtensionExceedingMaxLength() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setFileExt("A".repeat(17)); // Max is 16

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("File extension must not exceed 16 characters");
        }

        @Test
        @DisplayName("Should accept common file extensions")
        void shouldAcceptCommonFileExtensions() {
            // Given
            String[] validExtensions = {"pdf", "jpg", "png", "docx", "xlsx", "txt", "csv"};

            for (String ext : validExtensions) {
                Attachment attachment = createValidAttachment();
                attachment.setFileExt(ext);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("SHA256 Hash Validation Tests")
    class SHA256ValidationTests {

        @Test
        @DisplayName("Should accept null SHA256 hash")
        void shouldAcceptNullSHA256() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setSha256(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject SHA256 hash exceeding 64 characters")
        void shouldRejectSHA256ExceedingMaxLength() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setSha256("a".repeat(65)); // Max is 64

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("SHA256 hash must be 64 characters");
        }

        @Test
        @DisplayName("Should accept valid 64-character SHA256 hash")
        void shouldAcceptValidSHA256Hash() {
            // Given
            Attachment attachment = createValidAttachment();
            String validHash = "a".repeat(64);
            attachment.setSha256(validHash);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
            assertThat(attachment.getSha256()).hasSize(64);
        }
    }

    @Nested
    @DisplayName("Bucket Validation Tests")
    class BucketValidationTests {

        @Test
        @DisplayName("Should accept null bucket")
        void shouldAcceptNullBucket() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setBucket(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject bucket exceeding max length")
        void shouldRejectBucketExceedingMaxLength() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setBucket("A".repeat(129)); // Max is 128

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Bucket must not exceed 128 characters");
        }

        @Test
        @DisplayName("Should accept valid bucket names")
        void shouldAcceptValidBucketNames() {
            // Given
            String[] validBuckets = {"my-bucket", "uploads", "documents-2024", "prod-files"};

            for (String bucket : validBuckets) {
                Attachment attachment = createValidAttachment();
                attachment.setBucket(bucket);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Uploader Validation Tests")
    class UploaderValidationTests {

        @Test
        @DisplayName("Should reject null uploader")
        void shouldRejectNullUploader() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setUploadedBy(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Uploader cannot be null");
        }

        @Test
        @DisplayName("Should accept valid uploader IDs")
        void shouldAcceptValidUploaderIds() {
            // Given
            Long[] validIds = {1L, 100L, 999999L};

            for (Long id : validIds) {
                Attachment attachment = createValidAttachment();
                attachment.setUploadedBy(id);

                // When
                Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Upload Timestamp Validation Tests")
    class UploadTimestampValidationTests {

        @Test
        @DisplayName("Should reject null upload timestamp")
        void shouldRejectNullUploadTimestamp() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setUploadedAt(null);

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Upload timestamp cannot be null");
        }

        @Test
        @DisplayName("Should reject future upload timestamp")
        void shouldRejectFutureUploadTimestamp() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setUploadedAt(OffsetDateTime.now().plusDays(1));

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Upload timestamp cannot be in the future");
        }

        @Test
        @DisplayName("Should accept current timestamp")
        void shouldAcceptCurrentTimestamp() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setUploadedAt(OffsetDateTime.now());

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept past timestamp")
        void shouldAcceptPastTimestamp() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setUploadedAt(OffsetDateTime.now().minusDays(1));

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Soft Delete Tests")
    class SoftDeleteTests {

        @Test
        @DisplayName("Should support soft delete flag")
        void shouldSupportSoftDeleteFlag() {
            // Given
            Attachment attachment = createValidAttachment();
            attachment.setIsDeleted(true);
            attachment.setDeletedAt(OffsetDateTime.now());

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).isEmpty();
            assertThat(attachment.getIsDeleted()).isTrue();
            assertThat(attachment.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should default to not deleted")
        void shouldDefaultToNotDeleted() {
            // Given
            Attachment attachment = new Attachment();

            // When/Then
            assertThat(attachment.getIsDeleted()).isFalse();
            assertThat(attachment.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors Tests")
    class MultipleValidationErrorsTests {

        @Test
        @DisplayName("Should report multiple validation errors")
        void shouldReportMultipleValidationErrors() {
            // Given
            Attachment attachment = new Attachment();
            // Leave all required fields null/invalid

            // When
            Set<ConstraintViolation<Attachment>> violations = validator.validate(attachment);

            // Then
            assertThat(violations).hasSizeGreaterThanOrEqualTo(4); // At least 4 required fields
            
            // Verify specific violations exist
            Set<String> messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(java.util.stream.Collectors.toSet());
            
            assertThat(messages).anyMatch(msg -> msg.contains("Object key"));
            assertThat(messages).anyMatch(msg -> msg.contains("Original name"));
            assertThat(messages).anyMatch(msg -> msg.contains("File size"));
            assertThat(messages).anyMatch(msg -> msg.contains("Uploader"));
        }
    }
}
