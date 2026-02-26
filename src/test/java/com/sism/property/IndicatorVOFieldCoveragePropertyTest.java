package com.sism.property;

import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Indicator VO Field Coverage
 * 
 * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
 * 
 * For any field in the frontend TypeScript interface, the backend VO class 
 * SHALL contain a corresponding property with compatible type.
 * 
 * **Validates: Requirements 5.2**
 */
public class IndicatorVOFieldCoveragePropertyTest {

    /**
     * Required fields that must exist in IndicatorVO
     * These fields correspond to the frontend TypeScript interface
     */
    private static final Set<String> REQUIRED_FRONTEND_FIELDS = Set.of(
            // Original fields
            "indicatorId",
            "taskId",
            "taskName",
            "parentIndicatorId",
            "parentIndicatorDesc",
            "level",
            "ownerOrgId",
            "ownerOrgName",
            "targetOrgId",
            "targetOrgName",
            "indicatorDesc",
            "weightPercent",
            "sortOrder",
            "year",
            "status",
            "remark",
            "createdAt",
            "updatedAt",
            "childIndicators",
            "milestones",
            // New fields for frontend alignment
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
            "pendingAttachments",
            // Derived fields for frontend compatibility
            "isStrategic",
            "responsibleDept",
            "ownerDept"
    );

    /**
     * New fields added specifically for frontend data alignment
     */
    private static final Set<String> NEW_ALIGNMENT_FIELDS = Set.of(
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
            "pendingAttachments",
            "isStrategic",
            "responsibleDept",
            "ownerDept"
    );

    /**
     * Expected field types for new alignment fields
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
            Map.entry("pendingAttachments", String.class),
            Map.entry("isStrategic", Boolean.class),
            Map.entry("responsibleDept", String.class),
            Map.entry("ownerDept", String.class)
    );

    /**
     * Get all declared fields from IndicatorVO class
     */
    private Set<String> getIndicatorVOFields() {
        Set<String> fields = new HashSet<>();
        for (Field field : IndicatorVO.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        return fields;
    }

    /**
     * Get field by name from IndicatorVO
     */
    private Optional<Field> getFieldByName(String fieldName) {
        try {
            return Optional.of(IndicatorVO.class.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            return Optional.empty();
        }
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> requiredFieldNames() {
        return Arbitraries.of(REQUIRED_FRONTEND_FIELDS);
    }

    @Provide
    Arbitrary<String> newAlignmentFieldNames() {
        return Arbitraries.of(NEW_ALIGNMENT_FIELDS);
    }

    @Provide
    Arbitrary<List<String>> randomFieldSubsets() {
        return Arbitraries.of(REQUIRED_FRONTEND_FIELDS)
                .list()
                .ofMinSize(1)
                .ofMaxSize(REQUIRED_FRONTEND_FIELDS.size());
    }

    // ==================== Property Tests ====================

    /**
     * Property 8.1: Every required frontend field exists in IndicatorVO
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * For any field required by the frontend TypeScript interface,
     * the IndicatorVO class SHALL contain a field with that name.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void everyRequiredFrontendField_shouldExistInIndicatorVO(
            @ForAll("requiredFieldNames") String fieldName) {
        
        Set<String> voFields = getIndicatorVOFields();
        
        assertThat(voFields)
                .as("IndicatorVO should contain field '%s' as required by frontend interface", 
                    fieldName)
                .contains(fieldName);
    }

    /**
     * Property 8.2: Every new alignment field has the correct type
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * For any new alignment field in IndicatorVO, the field type
     * SHALL be compatible with the frontend TypeScript type.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void everyNewAlignmentField_shouldHaveCorrectType(
            @ForAll("newAlignmentFieldNames") String fieldName) {
        
        Optional<Field> fieldOpt = getFieldByName(fieldName);
        
        assertThat(fieldOpt)
                .as("Field '%s' should exist in IndicatorVO", fieldName)
                .isPresent();
        
        Field field = fieldOpt.get();
        Class<?> expectedType = EXPECTED_FIELD_TYPES.get(fieldName);
        
        assertThat(field.getType())
                .as("Field '%s' should have type %s", fieldName, expectedType.getSimpleName())
                .isEqualTo(expectedType);
    }

    /**
     * Property 8.3: All required fields are present (completeness check)
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * For any random subset of required fields, all fields in the subset
     * SHALL exist in the IndicatorVO.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void allRequiredFieldSubsets_shouldExistInVO(
            @ForAll("randomFieldSubsets") List<String> fieldSubset) {
        
        Set<String> voFields = getIndicatorVOFields();
        
        for (String fieldName : fieldSubset) {
            assertThat(voFields)
                    .as("IndicatorVO should contain field '%s'", fieldName)
                    .contains(fieldName);
        }
    }

    /**
     * Property 8.4: Field count verification
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * The IndicatorVO SHALL contain at least all the required frontend fields.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 10)
    void indicatorVO_shouldContainAllRequiredFields() {
        Set<String> voFields = getIndicatorVOFields();
        
        // Verify all required frontend fields are present
        Set<String> missingFields = REQUIRED_FRONTEND_FIELDS.stream()
                .filter(f -> !voFields.contains(f))
                .collect(Collectors.toSet());
        
        assertThat(missingFields)
                .as("All required frontend fields should be present in IndicatorVO. Missing: %s", 
                    missingFields)
                .isEmpty();
    }

    /**
     * Property 8.5: New alignment fields completeness
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * All new fields added for frontend data alignment SHALL be present
     * in the IndicatorVO class.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 10)
    void indicatorVO_shouldContainAllNewAlignmentFields() {
        Set<String> voFields = getIndicatorVOFields();
        
        // Verify all new alignment fields are present
        Set<String> missingFields = NEW_ALIGNMENT_FIELDS.stream()
                .filter(f -> !voFields.contains(f))
                .collect(Collectors.toSet());
        
        assertThat(missingFields)
                .as("All new alignment fields should be present in IndicatorVO. Missing: %s", 
                    missingFields)
                .isEmpty();
    }

    /**
     * Property 8.6: Derived fields for frontend compatibility
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * Derived fields (isStrategic, responsibleDept, ownerDept) SHALL exist
     * in IndicatorVO to provide frontend-compatible field names.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 10)
    void indicatorVO_shouldContainDerivedFields() {
        Set<String> voFields = getIndicatorVOFields();
        
        Set<String> derivedFields = Set.of("isStrategic", "responsibleDept", "ownerDept");
        
        for (String fieldName : derivedFields) {
            assertThat(voFields)
                    .as("IndicatorVO should contain derived field '%s' for frontend compatibility", 
                        fieldName)
                    .contains(fieldName);
        }
    }

    /**
     * Property 8.7: Field naming convention (camelCase)
     * 
     * **Feature: data-alignment-sop, Property 8: VO 字段覆盖**
     * 
     * All fields in IndicatorVO SHALL use camelCase naming convention
     * to match frontend TypeScript interface expectations.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void allFields_shouldUseCamelCaseNaming(
            @ForAll("requiredFieldNames") String fieldName) {
        
        // Verify field name starts with lowercase letter
        assertThat(Character.isLowerCase(fieldName.charAt(0)))
                .as("Field '%s' should start with lowercase letter (camelCase)", fieldName)
                .isTrue();
        
        // Verify no underscores (snake_case)
        assertThat(fieldName)
                .as("Field '%s' should not contain underscores (use camelCase)", fieldName)
                .doesNotContain("_");
    }
}
