package com.sism.property;

import com.sism.entity.Indicator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Indicator Entity Field Coverage
 * 
 * **Feature: data-alignment-sop, Property 7: Entity 字段覆盖**
 * 
 * For any database column added during schema sync, the backend Entity class 
 * SHALL contain a corresponding Java property.
 * 
 * **Validates: Requirements 5.1**
 */
public class IndicatorEntityFieldCoveragePropertyTest {

    /**
     * Required fields that must exist in Indicator Entity
     * These are the fields added during the data alignment process
     */
    private static final Set<String> REQUIRED_NEW_FIELDS = Set.of(
            "isQualitative",
            "type1",
            "type2",
            "canWithdraw",
            "targetValue",
            "actualValue",
            "unit",
            "responsiblePerson",
            "progress",
            "statusAudit",
            "progressApprovalStatus",
            "pendingProgress",
            "pendingRemark",
            "pendingAttachments"
    );

    /**
     * Expected field types for validation
     */
    private static final Map<String, Class<?>> EXPECTED_FIELD_TYPES = Map.ofEntries(
            Map.entry("isQualitative", Boolean.class),
            Map.entry("type1", String.class),
            Map.entry("type2", String.class),
            Map.entry("canWithdraw", Boolean.class),
            Map.entry("targetValue", java.math.BigDecimal.class),
            Map.entry("actualValue", java.math.BigDecimal.class),
            Map.entry("unit", String.class),
            Map.entry("responsiblePerson", String.class),
            Map.entry("progress", Integer.class),
            Map.entry("statusAudit", String.class),
            Map.entry("progressApprovalStatus", com.sism.enums.ProgressApprovalStatus.class),
            Map.entry("pendingProgress", Integer.class),
            Map.entry("pendingRemark", String.class),
            Map.entry("pendingAttachments", String.class)
    );

    /**
     * Get all declared fields from Indicator entity class
     */
    private Set<String> getIndicatorEntityFields() {
        Set<String> fields = new HashSet<>();
        for (Field field : Indicator.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        return fields;
    }

    /**
     * Get field by name from Indicator entity
     */
    private Optional<Field> getFieldByName(String fieldName) {
        try {
            return Optional.of(Indicator.class.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            return Optional.empty();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> requiredFieldNames() {
        return Arbitraries.of(REQUIRED_NEW_FIELDS);
    }

    @Provide
    Arbitrary<List<String>> randomFieldSubsets() {
        return Arbitraries.of(REQUIRED_NEW_FIELDS)
                .list()
                .ofMinSize(1)
                .ofMaxSize(REQUIRED_NEW_FIELDS.size());
    }

    // ==================== Property Tests ====================

    /**
     * Property 7.1: Every required new field exists in Indicator Entity
     * 
     * **Feature: data-alignment-sop, Property 7: Entity 字段覆盖**
     * 
     * For any required field name from the data alignment specification,
     * the Indicator Entity class SHALL contain a field with that name.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    void everyRequiredField_shouldExistInIndicatorEntity(
            @ForAll("requiredFieldNames") String fieldName) {
        
        Set<String> entityFields = getIndicatorEntityFields();
        
        assertThat(entityFields)
                .as("Indicator Entity should contain field '%s' as required by data alignment spec", 
                    fieldName)
                .contains(fieldName);
    }

    /**
     * Property 7.2: Every required field has the correct type
     * 
     * **Feature: data-alignment-sop, Property 7: Entity 字段覆盖**
     * 
     * For any required field in the Indicator Entity, the field type
     * SHALL match the expected type from the data alignment specification.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    void everyRequiredField_shouldHaveCorrectType(
            @ForAll("requiredFieldNames") String fieldName) {
        
        Optional<Field> fieldOpt = getFieldByName(fieldName);
        
        assertThat(fieldOpt)
                .as("Field '%s' should exist in Indicator Entity", fieldName)
                .isPresent();
        
        Field field = fieldOpt.get();
        Class<?> expectedType = EXPECTED_FIELD_TYPES.get(fieldName);
        
        assertThat(field.getType())
                .as("Field '%s' should have type %s", fieldName, expectedType.getSimpleName())
                .isEqualTo(expectedType);
    }

    /**
     * Property 7.3: All required fields are present (completeness check)
     * 
     * **Feature: data-alignment-sop, Property 7: Entity 字段覆盖**
     * 
     * For any random subset of required fields, all fields in the subset
     * SHALL exist in the Indicator Entity.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    void allRequiredFieldSubsets_shouldExistInEntity(
            @ForAll("randomFieldSubsets") List<String> fieldSubset) {
        
        Set<String> entityFields = getIndicatorEntityFields();
        
        for (String fieldName : fieldSubset) {
            assertThat(entityFields)
                    .as("Indicator Entity should contain field '%s'", fieldName)
                    .contains(fieldName);
        }
    }

    /**
     * Property 7.4: Field count verification
     * 
     * **Feature: data-alignment-sop, Property 7: Entity 字段覆盖**
     * 
     * The Indicator Entity SHALL contain at least all the required new fields
     * plus the original fields.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 10)
    void indicatorEntity_shouldContainAllRequiredFields() {
        Set<String> entityFields = getIndicatorEntityFields();
        
        // Verify all required new fields are present
        Set<String> missingFields = REQUIRED_NEW_FIELDS.stream()
                .filter(f -> !entityFields.contains(f))
                .collect(Collectors.toSet());
        
        assertThat(missingFields)
                .as("All required new fields should be present in Indicator Entity. Missing: %s", 
                    missingFields)
                .isEmpty();
    }

    /**
     * Property 7.5: JPA annotation presence
     * 
     * **Feature: data-alignment-sop, Property 7: Entity 字段覆盖**
     * 
     * For any required field that maps to a database column, the field
     * SHALL have appropriate JPA annotations.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    void requiredFields_shouldHaveJpaAnnotations(
            @ForAll("requiredFieldNames") String fieldName) {
        
        Optional<Field> fieldOpt = getFieldByName(fieldName);
        assertThat(fieldOpt).isPresent();
        
        Field field = fieldOpt.get();
        
        // Check for @Column or @Enumerated annotation
        boolean hasColumnAnnotation = field.isAnnotationPresent(jakarta.persistence.Column.class);
        boolean hasEnumeratedAnnotation = field.isAnnotationPresent(jakarta.persistence.Enumerated.class);
        boolean hasJdbcTypeCode = field.isAnnotationPresent(org.hibernate.annotations.JdbcTypeCode.class);
        
        // At least one JPA-related annotation should be present for database-mapped fields
        boolean hasJpaAnnotation = hasColumnAnnotation || hasEnumeratedAnnotation || hasJdbcTypeCode;
        
        assertThat(hasJpaAnnotation)
                .as("Field '%s' should have JPA annotation (@Column, @Enumerated, or @JdbcTypeCode)", 
                    fieldName)
                .isTrue();
    }
}
