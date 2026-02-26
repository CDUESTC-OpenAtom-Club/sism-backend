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

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AuditFlowDef entity
 * Tests entity creation and validation constraints
 * 
 * Requirements: Task 2.2 - Audit flow entities validation
 */
@DisplayName("AuditFlowDef Entity Tests")
class AuditFlowDefEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Helper method to create a valid AuditFlowDef entity
     */
    private AuditFlowDef createValidAuditFlowDef() {
        AuditFlowDef flowDef = new AuditFlowDef();
        flowDef.setFlowName("Indicator Approval Flow");
        flowDef.setFlowCode("INDICATOR_APPROVAL");
        flowDef.setEntityType(AuditEntityType.INDICATOR);
        flowDef.setDescription("Approval workflow for strategic indicators");
        return flowDef;
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {

        @Test
        @DisplayName("Should create audit flow def with all required fields")
        void shouldCreateAuditFlowDefWithRequiredFields() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).isEmpty();
            assertThat(flowDef.getFlowName()).isEqualTo("Indicator Approval Flow");
            assertThat(flowDef.getFlowCode()).isEqualTo("INDICATOR_APPROVAL");
            assertThat(flowDef.getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
            assertThat(flowDef.getDescription()).isEqualTo("Approval workflow for strategic indicators");
        }

        @Test
        @DisplayName("Should create audit flow def without optional description")
        void shouldCreateAuditFlowDefWithoutDescription() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setDescription(null);

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).isEmpty();
            assertThat(flowDef.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Flow Name Validation Tests")
    class FlowNameValidationTests {

        @Test
        @DisplayName("Should reject null flow name")
        void shouldRejectNullFlowName() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowName(null);

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow name is required");
        }

        @Test
        @DisplayName("Should reject blank flow name")
        void shouldRejectBlankFlowName() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowName("");

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow name is required");
        }

        @Test
        @DisplayName("Should reject flow name exceeding max length")
        void shouldRejectFlowNameExceedingMaxLength() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowName("A".repeat(101)); // Max is 100

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow name must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should accept flow name at max length")
        void shouldAcceptFlowNameAtMaxLength() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowName("A".repeat(100)); // Exactly 100

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Flow Code Validation Tests")
    class FlowCodeValidationTests {

        @Test
        @DisplayName("Should reject null flow code")
        void shouldRejectNullFlowCode() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowCode(null);

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow code is required");
        }

        @Test
        @DisplayName("Should reject blank flow code")
        void shouldRejectBlankFlowCode() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowCode("");

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow code is required");
        }

        @Test
        @DisplayName("Should reject flow code exceeding max length")
        void shouldRejectFlowCodeExceedingMaxLength() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setFlowCode("A".repeat(51)); // Max is 50

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Flow code must not exceed 50 characters");
        }

        @Test
        @DisplayName("Should accept valid flow codes")
        void shouldAcceptValidFlowCodes() {
            // Given
            String[] validCodes = {
                "INDICATOR_APPROVAL",
                "TASK_APPROVAL",
                "REPORT_APPROVAL",
                "MILESTONE_APPROVAL"
            };

            for (String code : validCodes) {
                AuditFlowDef flowDef = createValidAuditFlowDef();
                flowDef.setFlowCode(code);

                // When
                Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

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
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setEntityType(null);

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

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
                AuditFlowDef flowDef = createValidAuditFlowDef();
                flowDef.setEntityType(type);

                // When
                Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

                // Then
                assertThat(violations).isEmpty();
                assertThat(flowDef.getEntityType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("Description Validation Tests")
    class DescriptionValidationTests {

        @Test
        @DisplayName("Should accept null description")
        void shouldAcceptNullDescription() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setDescription(null);

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept empty description")
        void shouldAcceptEmptyDescription() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setDescription("");

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject description exceeding max length")
        void shouldRejectDescriptionExceedingMaxLength() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setDescription("A".repeat(501)); // Max is 500

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Description must not exceed 500 characters");
        }

        @Test
        @DisplayName("Should accept description at max length")
        void shouldAcceptDescriptionAtMaxLength() {
            // Given
            AuditFlowDef flowDef = createValidAuditFlowDef();
            flowDef.setDescription("A".repeat(500)); // Exactly 500

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors Tests")
    class MultipleValidationErrorsTests {

        @Test
        @DisplayName("Should report multiple validation errors")
        void shouldReportMultipleValidationErrors() {
            // Given
            AuditFlowDef flowDef = new AuditFlowDef();
            // Leave all required fields null

            // When
            Set<ConstraintViolation<AuditFlowDef>> violations = validator.validate(flowDef);

            // Then
            assertThat(violations).hasSize(3); // 3 required fields
            
            // Verify specific violations exist
            Set<String> messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(java.util.stream.Collectors.toSet());
            
            assertThat(messages).anyMatch(msg -> msg.contains("Flow name"));
            assertThat(messages).anyMatch(msg -> msg.contains("Flow code"));
            assertThat(messages).anyMatch(msg -> msg.contains("Entity type"));
        }
    }
}
