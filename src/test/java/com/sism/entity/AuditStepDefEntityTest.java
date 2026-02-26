package com.sism.entity;

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
 * Unit tests for AuditStepDef entity
 * Tests entity creation and validation constraints
 * 
 * Requirements: Task 2.2 - Audit flow entities validation
 */
@DisplayName("AuditStepDef Entity Tests")
class AuditStepDefEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Helper method to create a valid AuditStepDef entity
     */
    private AuditStepDef createValidAuditStepDef() {
        AuditStepDef stepDef = new AuditStepDef();
        stepDef.setFlowId(1L);
        stepDef.setStepOrder(1);
        stepDef.setStepName("Functional Department Review");
        stepDef.setApproverRole("functional_dept");
        stepDef.setIsRequired(true);
        return stepDef;
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {

        @Test
        @DisplayName("Should create audit step def with all required fields")
        void shouldCreateAuditStepDefWithRequiredFields() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).isEmpty();
            assertThat(stepDef.getFlowId()).isEqualTo(1L);
            assertThat(stepDef.getStepOrder()).isEqualTo(1);
            assertThat(stepDef.getStepName()).isEqualTo("Functional Department Review");
            assertThat(stepDef.getApproverRole()).isEqualTo("functional_dept");
            assertThat(stepDef.getIsRequired()).isTrue();
        }

        @Test
        @DisplayName("Should set default value for isRequired")
        void shouldSetDefaultValueForIsRequired() {
            // Given/When
            AuditStepDef stepDef = new AuditStepDef();

            // Then
            assertThat(stepDef.getIsRequired()).isTrue();
        }

        @Test
        @DisplayName("Should create optional audit step")
        void shouldCreateOptionalAuditStep() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setIsRequired(false);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).isEmpty();
            assertThat(stepDef.getIsRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Flow ID Validation Tests")
    class FlowIdValidationTests {

        @Test
        @DisplayName("Should reject null flow ID")
        void shouldRejectNullFlowId() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setFlowId(null);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

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
                AuditStepDef stepDef = createValidAuditStepDef();
                stepDef.setFlowId(id);

                // When
                Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Step Order Validation Tests")
    class StepOrderValidationTests {

        @Test
        @DisplayName("Should reject null step order")
        void shouldRejectNullStepOrder() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setStepOrder(null);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Step order is required");
        }

        @Test
        @DisplayName("Should reject step order less than 1")
        void shouldRejectStepOrderLessThanOne() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setStepOrder(0);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Step order must be at least 1");
        }

        @Test
        @DisplayName("Should reject negative step order")
        void shouldRejectNegativeStepOrder() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setStepOrder(-1);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Step order must be at least 1");
        }

        @Test
        @DisplayName("Should accept valid step orders")
        void shouldAcceptValidStepOrders() {
            // Given
            Integer[] validOrders = {1, 2, 3, 10, 100};

            for (Integer order : validOrders) {
                AuditStepDef stepDef = createValidAuditStepDef();
                stepDef.setStepOrder(order);

                // When
                Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Step Name Validation Tests")
    class StepNameValidationTests {

        @Test
        @DisplayName("Should reject null step name")
        void shouldRejectNullStepName() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setStepName(null);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Step name is required");
        }

        @Test
        @DisplayName("Should reject blank step name")
        void shouldRejectBlankStepName() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setStepName("");

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Step name is required");
        }

        @Test
        @DisplayName("Should reject step name exceeding max length")
        void shouldRejectStepNameExceedingMaxLength() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setStepName("A".repeat(101)); // Max is 100

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Step name must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should accept valid step names")
        void shouldAcceptValidStepNames() {
            // Given
            String[] validNames = {
                "Functional Department Review",
                "Strategic Department Approval",
                "Secondary College Submission",
                "Final Review"
            };

            for (String name : validNames) {
                AuditStepDef stepDef = createValidAuditStepDef();
                stepDef.setStepName(name);

                // When
                Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Approver Role Validation Tests")
    class ApproverRoleValidationTests {

        @Test
        @DisplayName("Should reject null approver role")
        void shouldRejectNullApproverRole() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setApproverRole(null);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Approver role is required");
        }

        @Test
        @DisplayName("Should reject blank approver role")
        void shouldRejectBlankApproverRole() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setApproverRole("");

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Approver role is required");
        }

        @Test
        @DisplayName("Should reject approver role exceeding max length")
        void shouldRejectApproverRoleExceedingMaxLength() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setApproverRole("A".repeat(51)); // Max is 50

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Approver role must not exceed 50 characters");
        }

        @Test
        @DisplayName("Should accept valid approver roles")
        void shouldAcceptValidApproverRoles() {
            // Given
            String[] validRoles = {
                "strategic_dept",
                "functional_dept",
                "secondary_college"
            };

            for (String role : validRoles) {
                AuditStepDef stepDef = createValidAuditStepDef();
                stepDef.setApproverRole(role);

                // When
                Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Is Required Validation Tests")
    class IsRequiredValidationTests {

        @Test
        @DisplayName("Should reject null isRequired")
        void shouldRejectNullIsRequired() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setIsRequired(null);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Is required flag is required");
        }

        @Test
        @DisplayName("Should accept true for isRequired")
        void shouldAcceptTrueForIsRequired() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setIsRequired(true);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).isEmpty();
            assertThat(stepDef.getIsRequired()).isTrue();
        }

        @Test
        @DisplayName("Should accept false for isRequired")
        void shouldAcceptFalseForIsRequired() {
            // Given
            AuditStepDef stepDef = createValidAuditStepDef();
            stepDef.setIsRequired(false);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).isEmpty();
            assertThat(stepDef.getIsRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors Tests")
    class MultipleValidationErrorsTests {

        @Test
        @DisplayName("Should report multiple validation errors")
        void shouldReportMultipleValidationErrors() {
            // Given
            AuditStepDef stepDef = new AuditStepDef();
            // Leave all required fields null except isRequired (has default)

            // When
            Set<ConstraintViolation<AuditStepDef>> violations = validator.validate(stepDef);

            // Then
            assertThat(violations).hasSize(4); // 4 required fields (flowId, stepOrder, stepName, approverRole)
            
            // Verify specific violations exist
            Set<String> messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(java.util.stream.Collectors.toSet());
            
            assertThat(messages).anyMatch(msg -> msg.contains("Flow ID"));
            assertThat(messages).anyMatch(msg -> msg.contains("Step order"));
            assertThat(messages).anyMatch(msg -> msg.contains("Step name"));
            assertThat(messages).anyMatch(msg -> msg.contains("Approver role"));
        }
    }

    @Nested
    @DisplayName("Multi-Step Workflow Tests")
    class MultiStepWorkflowTests {

        @Test
        @DisplayName("Should support sequential step ordering")
        void shouldSupportSequentialStepOrdering() {
            // Given - Create a 3-step workflow
            AuditStepDef step1 = createValidAuditStepDef();
            step1.setStepOrder(1);
            step1.setStepName("Initial Review");
            step1.setApproverRole("functional_dept");

            AuditStepDef step2 = createValidAuditStepDef();
            step2.setStepOrder(2);
            step2.setStepName("Department Approval");
            step2.setApproverRole("strategic_dept");

            AuditStepDef step3 = createValidAuditStepDef();
            step3.setStepOrder(3);
            step3.setStepName("Final Approval");
            step3.setApproverRole("strategic_dept");

            // When
            Set<ConstraintViolation<AuditStepDef>> violations1 = validator.validate(step1);
            Set<ConstraintViolation<AuditStepDef>> violations2 = validator.validate(step2);
            Set<ConstraintViolation<AuditStepDef>> violations3 = validator.validate(step3);

            // Then
            assertThat(violations1).isEmpty();
            assertThat(violations2).isEmpty();
            assertThat(violations3).isEmpty();
            assertThat(step1.getStepOrder()).isLessThan(step2.getStepOrder());
            assertThat(step2.getStepOrder()).isLessThan(step3.getStepOrder());
        }

        @Test
        @DisplayName("Should support mixed required and optional steps")
        void shouldSupportMixedRequiredAndOptionalSteps() {
            // Given
            AuditStepDef requiredStep = createValidAuditStepDef();
            requiredStep.setStepOrder(1);
            requiredStep.setIsRequired(true);

            AuditStepDef optionalStep = createValidAuditStepDef();
            optionalStep.setStepOrder(2);
            optionalStep.setIsRequired(false);

            // When
            Set<ConstraintViolation<AuditStepDef>> violations1 = validator.validate(requiredStep);
            Set<ConstraintViolation<AuditStepDef>> violations2 = validator.validate(optionalStep);

            // Then
            assertThat(violations1).isEmpty();
            assertThat(violations2).isEmpty();
            assertThat(requiredStep.getIsRequired()).isTrue();
            assertThat(optionalStep.getIsRequired()).isFalse();
        }
    }
}
