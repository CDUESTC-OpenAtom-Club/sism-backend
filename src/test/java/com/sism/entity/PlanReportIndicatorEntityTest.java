package com.sism.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PlanReportIndicator entity
 */
@DisplayName("PlanReportIndicator Entity Tests")
class PlanReportIndicatorEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid PlanReportIndicator with all fields")
    void shouldCreateValidPlanReportIndicator() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .id(1L)
            .reportId(100L)
            .indicatorId(200L)
            .progress(75)
            .milestoneNote("Milestone achieved")
            .comment("Good progress")
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).isEmpty();
        assertThat(entity.getReportId()).isEqualTo(100L);
        assertThat(entity.getIndicatorId()).isEqualTo(200L);
        assertThat(entity.getProgress()).isEqualTo(75);
        assertThat(entity.getMilestoneNote()).isEqualTo("Milestone achieved");
        assertThat(entity.getComment()).isEqualTo("Good progress");
    }

    @Test
    @DisplayName("Should fail validation when reportId is null")
    void shouldFailValidationWhenReportIdIsNull() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(null)
            .indicatorId(200L)
            .progress(50)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Report ID is required");
    }

    @Test
    @DisplayName("Should fail validation when indicatorId is null")
    void shouldFailValidationWhenIndicatorIdIsNull() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(null)
            .progress(50)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Indicator ID is required");
    }

    @Test
    @DisplayName("Should fail validation when progress is null")
    void shouldFailValidationWhenProgressIsNull() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(null)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Progress is required");
    }

    @Test
    @DisplayName("Should fail validation when progress is negative")
    void shouldFailValidationWhenProgressIsNegative() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(-1)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Progress must be at least 0");
    }

    @Test
    @DisplayName("Should fail validation when progress exceeds 100")
    void shouldFailValidationWhenProgressExceeds100() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(101)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Progress must not exceed 100");
    }

    @Test
    @DisplayName("Should accept progress at boundary values 0 and 100")
    void shouldAcceptProgressAtBoundaryValues() {
        // Test progress = 0
        PlanReportIndicator entity0 = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(0)
            .createdAt(OffsetDateTime.now())
            .build();

        Set<ConstraintViolation<PlanReportIndicator>> violations0 = validator.validate(entity0);
        assertThat(violations0).isEmpty();

        // Test progress = 100
        PlanReportIndicator entity100 = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(100)
            .createdAt(OffsetDateTime.now())
            .build();

        Set<ConstraintViolation<PlanReportIndicator>> violations100 = validator.validate(entity100);
        assertThat(violations100).isEmpty();
    }

    @Test
    @DisplayName("Should allow null milestone note and comment")
    void shouldAllowNullMilestoneNoteAndComment() {
        // Given
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(50)
            .milestoneNote(null)
            .comment(null)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicator>> violations = validator.validate(entity);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should set default values in @PrePersist")
    void shouldSetDefaultValuesInPrePersist() {
        // Given
        PlanReportIndicator entity = new PlanReportIndicator();
        entity.setReportId(100L);
        entity.setIndicatorId(200L);

        // When
        entity.onCreate();

        // Then
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getProgress()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not override existing values in @PrePersist")
    void shouldNotOverrideExistingValuesInPrePersist() {
        // Given
        OffsetDateTime existingTime = OffsetDateTime.now().minusDays(1);
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(75)
            .createdAt(existingTime)
            .build();

        // When
        entity.onCreate();

        // Then
        assertThat(entity.getCreatedAt()).isEqualTo(existingTime);
        assertThat(entity.getProgress()).isEqualTo(75);
    }

    @Test
    @DisplayName("Should support builder pattern")
    void shouldSupportBuilderPattern() {
        // When
        PlanReportIndicator entity = PlanReportIndicator.builder()
            .reportId(100L)
            .indicatorId(200L)
            .progress(50)
            .milestoneNote("Test note")
            .comment("Test comment")
            .build();

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getReportId()).isEqualTo(100L);
        assertThat(entity.getIndicatorId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Should support no-args constructor")
    void shouldSupportNoArgsConstructor() {
        // When
        PlanReportIndicator entity = new PlanReportIndicator();

        // Then
        assertThat(entity).isNotNull();
    }

    @Test
    @DisplayName("Should support all-args constructor")
    void shouldSupportAllArgsConstructor() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        PlanReportIndicator entity = new PlanReportIndicator(
            1L, 100L, 200L, 75, "Note", "Comment", now
        );

        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getReportId()).isEqualTo(100L);
        assertThat(entity.getIndicatorId()).isEqualTo(200L);
        assertThat(entity.getProgress()).isEqualTo(75);
        assertThat(entity.getMilestoneNote()).isEqualTo("Note");
        assertThat(entity.getComment()).isEqualTo("Comment");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }
}
