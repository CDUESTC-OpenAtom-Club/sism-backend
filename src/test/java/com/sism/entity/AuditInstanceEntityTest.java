package com.sism.entity;

import com.sism.enums.AuditEntityType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AuditInstance entity
 * Tests entity creation and validation constraints
 * 
 * Requirements: Task 2.2 - Audit flow entities validation
 */
@DisplayName("AuditInstance Entity Tests")
class AuditInstanceEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Helper method to create a valid AuditInstance entity
     */
    private AuditInstance createValidAuditInstance() {
        AuditInstance instance = new AuditInstance();
        instance.setFlowId(1L);
        instance.setEntityId(100L);
        instance.setEntityType(AuditEntityType.INDICATOR);
        instance.setCurrentStepId(1L);
        instance.setStatus("PENDING");
        instance.setInitiatedBy(1L);
        instance.setInitiatedAt(LocalDateTime.now());
        return instance;
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {

        @Test
        @DisplayName("Should create audit instance with all required fields")
        void shouldCreateAuditInstanceWithRequiredFields() {
            // Given
            AuditInstance instance = createValidAuditInstance();

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getFlowId()).isEqualTo(1L);
            assertThat(instance.getEntityId()).isEqualTo(100L);
            assertThat(instance.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(instance.getCurrentStepId()).isEqualTo(1L);
            assertThat(instance.getStatus()).isEqualTo("PENDING");
            assertThat(instance.getInitiatedBy()).isEqualTo(1L);
            assertThat(instance.getInitiatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set default status to PENDING")
        void shouldSetDefaultStatusToPending() {
            // Given/When
            AuditInstance instance = new AuditInstance();

            // Then
            assertThat(instance.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Should create audit instance without optional completedAt")
        void shouldCreateAuditInstanceWithoutCompletedAt() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setCompletedAt(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("Should create completed audit instance")
        void shouldCreateCompletedAuditInstance() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("COMPLETED");
            instance.setCompletedAt(LocalDateTime.now());

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getStatus()).isEqualTo("COMPLETED");
            assertThat(instance.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Flow ID Validation Tests")
    class FlowIdValidationTests {

        @Test
        @DisplayName("Should reject null flow ID")
        void shouldRejectNullFlowId() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setFlowId(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow ID is required");
        }

        @Test
        @DisplayName("Should accept valid flow IDs")
        void shouldAcceptValidFlowIds() {
            // Given
            Long[] validIds = {1L, 100L, 999999L};

            for (Long id : validIds) {
                AuditInstance instance = createValidAuditInstance();
                instance.setFlowId(id);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Entity ID Validation Tests")
    class EntityIdValidationTests {

        @Test
        @DisplayName("Should reject null entity ID")
        void shouldRejectNullEntityId() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setEntityId(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Entity ID is required");
        }

        @Test
        @DisplayName("Should accept valid entity IDs")
        void shouldAcceptValidEntityIds() {
            // Given
            Long[] validIds = {1L, 100L, 999999L};

            for (Long id : validIds) {
                AuditInstance instance = createValidAuditInstance();
                instance.setEntityId(id);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Entity Type Validation Tests")
    class EntityTypeValidationTests {

        @Test
        @DisplayName("Should reject null entity type")
        void shouldRejectNullEntityType() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setEntityType(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Entity type is required");
        }

        @Test
        @DisplayName("Should accept all valid entity types")
        void shouldAcceptAllValidEntityTypes() {
            // Given
            AuditEntityType[] validTypes = AuditEntityType.values();

            for (AuditEntityType type : validTypes) {
                AuditInstance instance = createValidAuditInstance();
                instance.setEntityType(type);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
                assertThat(instance.getEntityType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("Current Step ID Validation Tests")
    class CurrentStepIdValidationTests {

        @Test
        @DisplayName("Should accept null current step ID")
        void shouldAcceptNullCurrentStepId() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setCurrentStepId(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept valid current step IDs")
        void shouldAcceptValidCurrentStepIds() {
            // Given
            Long[] validIds = {1L, 2L, 3L, 100L};

            for (Long id : validIds) {
                AuditInstance instance = createValidAuditInstance();
                instance.setCurrentStepId(id);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Status Validation Tests")
    class StatusValidationTests {

        @Test
        @DisplayName("Should reject null status")
        void shouldRejectNullStatus() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Status is required");
        }

        @Test
        @DisplayName("Should reject blank status")
        void shouldRejectBlankStatus() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("");

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Status is required");
        }

        @Test
        @DisplayName("Should reject status exceeding max length")
        void shouldRejectStatusExceedingMaxLength() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("A".repeat(51)); // Max is 50

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Status must not exceed 50 characters");
        }

        @Test
        @DisplayName("Should accept valid status values")
        void shouldAcceptValidStatusValues() {
            // Given
            String[] validStatuses = {
                "PENDING",
                "IN_PROGRESS",
                "APPROVED",
                "REJECTED",
                "COMPLETED",
                "CANCELLED"
            };

            for (String status : validStatuses) {
                AuditInstance instance = createValidAuditInstance();
                instance.setStatus(status);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Initiator Validation Tests")
    class InitiatorValidationTests {

        @Test
        @DisplayName("Should reject null initiatedBy")
        void shouldRejectNullInitiatedBy() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setInitiatedBy(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Initiated by is required");
        }

        @Test
        @DisplayName("Should accept valid initiator IDs")
        void shouldAcceptValidInitiatorIds() {
            // Given
            Long[] validIds = {1L, 100L, 999999L};

            for (Long id : validIds) {
                AuditInstance instance = createValidAuditInstance();
                instance.setInitiatedBy(id);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Timestamp Validation Tests")
    class TimestampValidationTests {

        @Test
        @DisplayName("Should reject null initiatedAt")
        void shouldRejectNullInitiatedAt() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setInitiatedAt(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Initiated at is required");
        }

        @Test
        @DisplayName("Should accept current timestamp for initiatedAt")
        void shouldAcceptCurrentTimestampForInitiatedAt() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setInitiatedAt(LocalDateTime.now());

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept past timestamp for initiatedAt")
        void shouldAcceptPastTimestampForInitiatedAt() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setInitiatedAt(LocalDateTime.now().minusDays(1));

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept null completedAt")
        void shouldAcceptNullCompletedAt() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setCompletedAt(null);

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept valid completedAt timestamp")
        void shouldAcceptValidCompletedAtTimestamp() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setCompletedAt(LocalDateTime.now());

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Workflow Lifecycle Tests")
    class WorkflowLifecycleTests {

        @Test
        @DisplayName("Should support pending to in-progress transition")
        void shouldSupportPendingToInProgressTransition() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("PENDING");
            instance.setCurrentStepId(1L);

            // When - Transition to in-progress
            instance.setStatus("IN_PROGRESS");
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Should support step progression")
        void shouldSupportStepProgression() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setCurrentStepId(1L);

            // When - Progress to next step
            instance.setCurrentStepId(2L);
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getCurrentStepId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should support workflow completion")
        void shouldSupportWorkflowCompletion() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("IN_PROGRESS");
            instance.setCompletedAt(null);

            // When - Complete workflow
            instance.setStatus("COMPLETED");
            instance.setCompletedAt(LocalDateTime.now());
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getStatus()).isEqualTo("COMPLETED");
            assertThat(instance.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should support workflow rejection")
        void shouldSupportWorkflowRejection() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("IN_PROGRESS");

            // When - Reject workflow
            instance.setStatus("REJECTED");
            instance.setCompletedAt(LocalDateTime.now());
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getStatus()).isEqualTo("REJECTED");
            assertThat(instance.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should support workflow cancellation")
        void shouldSupportWorkflowCancellation() {
            // Given
            AuditInstance instance = createValidAuditInstance();
            instance.setStatus("PENDING");

            // When - Cancel workflow
            instance.setStatus("CANCELLED");
            instance.setCompletedAt(LocalDateTime.now());
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).isEmpty();
            assertThat(instance.getStatus()).isEqualTo("CANCELLED");
        }
    }

    @Nested
    @DisplayName("Multiple Entity Type Tests")
    class MultipleEntityTypeTests {

        @Test
        @DisplayName("Should support audit instances for different entity types")
        void shouldSupportAuditInstancesForDifferentEntityTypes() {
            // Given - Create instances for different entity types
            AuditEntityType[] entityTypes = {
                AuditEntityType.INDICATOR,
                AuditEntityType.TASK,
                AuditEntityType.REPORT,
                AuditEntityType.MILESTONE
            };

            for (AuditEntityType type : entityTypes) {
                AuditInstance instance = createValidAuditInstance();
                instance.setEntityType(type);

                // When
                Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

                // Then
                assertThat(violations).isEmpty();
                assertThat(instance.getEntityType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors Tests")
    class MultipleValidationErrorsTests {

        @Test
        @DisplayName("Should report multiple validation errors")
        void shouldReportMultipleValidationErrors() {
            // Given
            AuditInstance instance = new AuditInstance();
            // Leave all required fields null except status (has default)

            // When
            Set<ConstraintViolation<AuditInstance>> violations = validator.validate(instance);

            // Then
            assertThat(violations).hasSize(5); // 5 required fields (flowId, entityId, entityType, initiatedBy, initiatedAt)
            
            // Verify specific violations exist
            Set<String> messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(java.util.stream.Collectors.toSet());
            
            assertThat(messages).anyMatch(msg -> msg.contains("Flow ID"));
            assertThat(messages).anyMatch(msg -> msg.contains("Entity ID"));
            assertThat(messages).anyMatch(msg -> msg.contains("Entity type"));
            assertThat(messages).anyMatch(msg -> msg.contains("Initiated by"));
            assertThat(messages).anyMatch(msg -> msg.contains("Initiated at"));
        }
    }
}
