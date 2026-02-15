package com.sism.entity;

import com.sism.enums.AlertSeverity;
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
 * Unit tests for WarnLevel entity
 * Tests entity creation and validation constraints
 * 
 * Requirements: Task 2.3 - WarnLevel entity validation
 */
@DisplayName("WarnLevel Entity Tests")
class WarnLevelEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Helper method to create a valid WarnLevel entity
     */
    private WarnLevel createValidWarnLevel() {
        WarnLevel warnLevel = new WarnLevel();
        warnLevel.setLevelName("High Warning");
        warnLevel.setLevelCode("HIGH_WARN");
        warnLevel.setThresholdValue(80);
        warnLevel.setSeverity(AlertSeverity.WARNING);
        warnLevel.setDescription("High warning level for indicators");
        warnLevel.setIsActive(true);
        return warnLevel;
    }

    @Nested
    @DisplayName("Entity Creation Tests")
    class EntityCreationTests {

        @Test
        @DisplayName("Should create warn level with all required fields")
        void shouldCreateWarnLevelWithRequiredFields() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getLevelName()).isEqualTo("High Warning");
            assertThat(warnLevel.getLevelCode()).isEqualTo("HIGH_WARN");
            assertThat(warnLevel.getThresholdValue()).isEqualTo(80);
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(warnLevel.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should create warn level with optional description")
        void shouldCreateWarnLevelWithOptionalDescription() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setDescription("Custom description for warning level");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getDescription()).isEqualTo("Custom description for warning level");
        }

        @Test
        @DisplayName("Should create warn level without description")
        void shouldCreateWarnLevelWithoutDescription() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setDescription(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getDescription()).isNull();
        }

        @Test
        @DisplayName("Should set default isActive to true")
        void shouldSetDefaultIsActiveToTrue() {
            // Given/When
            WarnLevel warnLevel = new WarnLevel();

            // Then
            assertThat(warnLevel.getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Level Name Validation Tests")
    class LevelNameValidationTests {

        @Test
        @DisplayName("Should reject null level name")
        void shouldRejectNullLevelName() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelName(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Level name is required");
        }

        @Test
        @DisplayName("Should reject blank level name")
        void shouldRejectBlankLevelName() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelName("");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Level name is required");
        }

        @Test
        @DisplayName("Should reject level name exceeding max length")
        void shouldRejectLevelNameExceedingMaxLength() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelName("A".repeat(101)); // Max is 100

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Level name must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should accept level name at max length")
        void shouldAcceptLevelNameAtMaxLength() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelName("A".repeat(100)); // Exactly 100

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Level Code Validation Tests")
    class LevelCodeValidationTests {

        @Test
        @DisplayName("Should reject null level code")
        void shouldRejectNullLevelCode() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelCode(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Level code is required");
        }

        @Test
        @DisplayName("Should reject blank level code")
        void shouldRejectBlankLevelCode() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelCode("");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Level code is required");
        }

        @Test
        @DisplayName("Should reject level code exceeding max length")
        void shouldRejectLevelCodeExceedingMaxLength() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelCode("A".repeat(51)); // Max is 50

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Level code must not exceed 50 characters");
        }

        @Test
        @DisplayName("Should accept level code at max length")
        void shouldAcceptLevelCodeAtMaxLength() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setLevelCode("A".repeat(50)); // Exactly 50

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Threshold Value Validation Tests")
    class ThresholdValueValidationTests {

        @Test
        @DisplayName("Should reject null threshold value")
        void shouldRejectNullThresholdValue() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setThresholdValue(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Threshold value is required");
        }

        @Test
        @DisplayName("Should reject negative threshold value")
        void shouldRejectNegativeThresholdValue() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setThresholdValue(-1);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Threshold value must be non-negative");
        }

        @Test
        @DisplayName("Should accept zero threshold value")
        void shouldAcceptZeroThresholdValue() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setThresholdValue(0);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getThresholdValue()).isZero();
        }

        @Test
        @DisplayName("Should accept positive threshold value")
        void shouldAcceptPositiveThresholdValue() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setThresholdValue(100);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getThresholdValue()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Severity Validation Tests")
    class SeverityValidationTests {

        @Test
        @DisplayName("Should reject null severity")
        void shouldRejectNullSeverity() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setSeverity(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Severity is required");
        }

        @Test
        @DisplayName("Should accept INFO severity")
        void shouldAcceptInfoSeverity() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setSeverity(AlertSeverity.INFO);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.INFO);
        }

        @Test
        @DisplayName("Should accept WARNING severity")
        void shouldAcceptWarningSeverity() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setSeverity(AlertSeverity.WARNING);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.WARNING);
        }

        @Test
        @DisplayName("Should accept CRITICAL severity")
        void shouldAcceptCriticalSeverity() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setSeverity(AlertSeverity.CRITICAL);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("Description Validation Tests")
    class DescriptionValidationTests {

        @Test
        @DisplayName("Should accept null description")
        void shouldAcceptNullDescription() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setDescription(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept empty description")
        void shouldAcceptEmptyDescription() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setDescription("");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject description exceeding max length")
        void shouldRejectDescriptionExceedingMaxLength() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setDescription("A".repeat(501)); // Max is 500

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .contains("Description must not exceed 500 characters");
        }

        @Test
        @DisplayName("Should accept description at max length")
        void shouldAcceptDescriptionAtMaxLength() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setDescription("A".repeat(500)); // Exactly 500

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Active Status Tests")
    class ActiveStatusTests {

        @Test
        @DisplayName("Should accept active warn level")
        void shouldAcceptActiveWarnLevel() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setIsActive(true);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should accept inactive warn level")
        void shouldAcceptInactiveWarnLevel() {
            // Given
            WarnLevel warnLevel = createValidWarnLevel();
            warnLevel.setIsActive(false);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getIsActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors Tests")
    class MultipleValidationErrorsTests {

        @Test
        @DisplayName("Should report multiple validation errors")
        void shouldReportMultipleValidationErrors() {
            // Given
            WarnLevel warnLevel = new WarnLevel();
            warnLevel.setLevelName(null);
            warnLevel.setLevelCode(null);
            warnLevel.setThresholdValue(null);
            warnLevel.setSeverity(null);

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(4);
        }

        @Test
        @DisplayName("Should report validation errors for invalid values")
        void shouldReportValidationErrorsForInvalidValues() {
            // Given
            WarnLevel warnLevel = new WarnLevel();
            warnLevel.setLevelName(""); // Blank
            warnLevel.setLevelCode("A".repeat(51)); // Too long
            warnLevel.setThresholdValue(-10); // Negative
            warnLevel.setSeverity(null); // Null

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should create INFO level warn level")
        void shouldCreateInfoLevelWarnLevel() {
            // Given
            WarnLevel warnLevel = new WarnLevel();
            warnLevel.setLevelName("Low Warning");
            warnLevel.setLevelCode("LOW_WARN");
            warnLevel.setThresholdValue(10);
            warnLevel.setSeverity(AlertSeverity.INFO);
            warnLevel.setDescription("Low warning level - gap <= 10%");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.INFO);
            assertThat(warnLevel.getThresholdValue()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should create WARNING level warn level")
        void shouldCreateWarningLevelWarnLevel() {
            // Given
            WarnLevel warnLevel = new WarnLevel();
            warnLevel.setLevelName("Medium Warning");
            warnLevel.setLevelCode("MED_WARN");
            warnLevel.setThresholdValue(20);
            warnLevel.setSeverity(AlertSeverity.WARNING);
            warnLevel.setDescription("Medium warning level - gap 10-20%");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(warnLevel.getThresholdValue()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should create CRITICAL level warn level")
        void shouldCreateCriticalLevelWarnLevel() {
            // Given
            WarnLevel warnLevel = new WarnLevel();
            warnLevel.setLevelName("High Warning");
            warnLevel.setLevelCode("HIGH_WARN");
            warnLevel.setThresholdValue(30);
            warnLevel.setSeverity(AlertSeverity.CRITICAL);
            warnLevel.setDescription("High warning level - gap > 20%");

            // When
            Set<ConstraintViolation<WarnLevel>> violations = validator.validate(warnLevel);

            // Then
            assertThat(violations).isEmpty();
            assertThat(warnLevel.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(warnLevel.getThresholdValue()).isEqualTo(30);
        }
    }
}
