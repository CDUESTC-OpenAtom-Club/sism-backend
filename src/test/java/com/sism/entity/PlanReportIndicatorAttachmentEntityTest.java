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
 * Unit tests for PlanReportIndicatorAttachment entity
 */
@DisplayName("PlanReportIndicatorAttachment Entity Tests")
class PlanReportIndicatorAttachmentEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid PlanReportIndicatorAttachment with all fields")
    void shouldCreateValidPlanReportIndicatorAttachment() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .id(1L)
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(1)
            .createdBy(300L)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).isEmpty();
        assertThat(entity.getPlanReportIndicatorId()).isEqualTo(100L);
        assertThat(entity.getAttachmentId()).isEqualTo(200L);
        assertThat(entity.getSortOrder()).isEqualTo(1);
        assertThat(entity.getCreatedBy()).isEqualTo(300L);
    }

    @Test
    @DisplayName("Should fail validation when planReportIndicatorId is null")
    void shouldFailValidationWhenPlanReportIndicatorIdIsNull() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(null)
            .attachmentId(200L)
            .sortOrder(1)
            .createdBy(300L)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Plan report indicator ID is required");
    }

    @Test
    @DisplayName("Should fail validation when attachmentId is null")
    void shouldFailValidationWhenAttachmentIdIsNull() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(null)
            .sortOrder(1)
            .createdBy(300L)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Attachment ID is required");
    }

    @Test
    @DisplayName("Should fail validation when sortOrder is null")
    void shouldFailValidationWhenSortOrderIsNull() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(null)
            .createdBy(300L)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Sort order is required");
    }

    @Test
    @DisplayName("Should fail validation when createdBy is null")
    void shouldFailValidationWhenCreatedByIsNull() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(1)
            .createdBy(null)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Created by is required");
    }

    @Test
    @DisplayName("Should fail validation when createdAt is null")
    void shouldFailValidationWhenCreatedAtIsNull() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(1)
            .createdBy(300L)
            .createdAt(null)
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Created at timestamp is required");
    }

    @Test
    @DisplayName("Should set default values in @PrePersist")
    void shouldSetDefaultValuesInPrePersist() {
        // Given
        PlanReportIndicatorAttachment entity = new PlanReportIndicatorAttachment();
        entity.setPlanReportIndicatorId(100L);
        entity.setAttachmentId(200L);
        entity.setCreatedBy(300L);

        // When
        entity.onCreate();

        // Then
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getSortOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not override existing values in @PrePersist")
    void shouldNotOverrideExistingValuesInPrePersist() {
        // Given
        OffsetDateTime existingTime = OffsetDateTime.now().minusDays(1);
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(5)
            .createdBy(300L)
            .createdAt(existingTime)
            .build();

        // When
        entity.onCreate();

        // Then
        assertThat(entity.getCreatedAt()).isEqualTo(existingTime);
        assertThat(entity.getSortOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should support builder pattern")
    void shouldSupportBuilderPattern() {
        // When
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(1)
            .createdBy(300L)
            .build();

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getPlanReportIndicatorId()).isEqualTo(100L);
        assertThat(entity.getAttachmentId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Should support no-args constructor")
    void shouldSupportNoArgsConstructor() {
        // When
        PlanReportIndicatorAttachment entity = new PlanReportIndicatorAttachment();

        // Then
        assertThat(entity).isNotNull();
    }

    @Test
    @DisplayName("Should support all-args constructor")
    void shouldSupportAllArgsConstructor() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        PlanReportIndicatorAttachment entity = new PlanReportIndicatorAttachment(
            1L, 100L, 200L, 1, 300L, now
        );

        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getPlanReportIndicatorId()).isEqualTo(100L);
        assertThat(entity.getAttachmentId()).isEqualTo(200L);
        assertThat(entity.getSortOrder()).isEqualTo(1);
        assertThat(entity.getCreatedBy()).isEqualTo(300L);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should allow different sort orders")
    void shouldAllowDifferentSortOrders() {
        // Test various sort orders
        for (int i = 0; i <= 10; i++) {
            PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
                .planReportIndicatorId(100L)
                .attachmentId(200L)
                .sortOrder(i)
                .createdBy(300L)
                .createdAt(OffsetDateTime.now())
                .build();

            Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("Should allow negative sort orders")
    void shouldAllowNegativeSortOrders() {
        // Given
        PlanReportIndicatorAttachment entity = PlanReportIndicatorAttachment.builder()
            .planReportIndicatorId(100L)
            .attachmentId(200L)
            .sortOrder(-1)
            .createdBy(300L)
            .createdAt(OffsetDateTime.now())
            .build();

        // When
        Set<ConstraintViolation<PlanReportIndicatorAttachment>> violations = validator.validate(entity);

        // Then
        assertThat(violations).isEmpty();
    }
}
