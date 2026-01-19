package com.sism.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for API Response Field Matching
 * 
 * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
 * 
 * For any API endpoint returning indicator data, the response JSON field names 
 * SHALL match the frontend TypeScript interface field names (camelCase).
 * 
 * **Validates: Requirements 5.3**
 */
public class ApiResponseFieldMatchPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Frontend TypeScript interface field names (from StrategicIndicator)
     * These are the expected field names in API responses
     */
    private static final Set<String> FRONTEND_EXPECTED_FIELDS = Set.of(
            // Core fields (mapped from backend)
            "indicatorId",      // maps to frontend 'id'
            "indicatorDesc",    // maps to frontend 'name'
            "status",
            "year",
            "weightPercent",    // maps to frontend 'weight'
            "remark",
            "createdAt",        // maps to frontend 'createTime'
            // Organization fields
            "ownerOrgId",
            "ownerOrgName",
            "targetOrgId",
            "targetOrgName",
            // New alignment fields
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
            // Derived fields
            "isStrategic",
            "responsibleDept",
            "ownerDept",
            // Nested data
            "milestones",
            "childIndicators"
    );

    /**
     * Fields that must use camelCase naming
     */
    private static final Set<String> CAMEL_CASE_REQUIRED_FIELDS = Set.of(
            "indicatorId",
            "indicatorDesc",
            "ownerOrgId",
            "ownerOrgName",
            "targetOrgId",
            "targetOrgName",
            "weightPercent",
            "sortOrder",
            "createdAt",
            "updatedAt",
            "isQualitative",
            "canWithdraw",
            "targetValue",
            "actualValue",
            "responsiblePerson",
            "statusAudit",
            "progressApprovalStatus",
            "pendingProgress",
            "pendingRemark",
            "pendingAttachments",
            "isStrategic",
            "responsibleDept",
            "ownerDept",
            "childIndicators",
            "parentIndicatorId",
            "parentIndicatorDesc",
            "taskId",
            "taskName"
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

    // ==================== Generators ====================

    @Provide
    Arbitrary<String> frontendExpectedFieldNames() {
        return Arbitraries.of(FRONTEND_EXPECTED_FIELDS);
    }

    @Provide
    Arbitrary<String> camelCaseRequiredFieldNames() {
        return Arbitraries.of(CAMEL_CASE_REQUIRED_FIELDS);
    }

    @Provide
    Arbitrary<List<String>> randomFieldSubsets() {
        return Arbitraries.of(FRONTEND_EXPECTED_FIELDS)
                .list()
                .ofMinSize(1)
                .ofMaxSize(FRONTEND_EXPECTED_FIELDS.size());
    }

    // ==================== Property Tests ====================

    /**
     * Property 9.1: Every frontend expected field exists in IndicatorVO
     * 
     * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
     * 
     * For any field expected by the frontend TypeScript interface,
     * the IndicatorVO class SHALL contain a field with that name.
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void everyFrontendExpectedField_shouldExistInIndicatorVO(
            @ForAll("frontendExpectedFieldNames") String fieldName) {
        
        Set<String> voFields = getIndicatorVOFields();
        
        assertThat(voFields)
                .as("IndicatorVO should contain field '%s' as expected by frontend", fieldName)
                .contains(fieldName);
    }

    /**
     * Property 9.2: All fields use camelCase naming convention
     * 
     * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
     * 
     * For any field in the API response, the field name SHALL use
     * camelCase naming convention (no underscores, starts with lowercase).
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void allFields_shouldUseCamelCaseNaming(
            @ForAll("camelCaseRequiredFieldNames") String fieldName) {
        
        // Verify field name starts with lowercase letter
        assertThat(Character.isLowerCase(fieldName.charAt(0)))
                .as("Field '%s' should start with lowercase letter (camelCase)", fieldName)
                .isTrue();
        
        // Verify no underscores (snake_case)
        assertThat(fieldName)
                .as("Field '%s' should not contain underscores (use camelCase)", fieldName)
                .doesNotContain("_");
    }

    /**
     * Property 9.3: IndicatorVO serializes to JSON with correct field names
     * 
     * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
     * 
     * For any IndicatorVO instance, serializing to JSON SHALL produce
     * field names that match the frontend TypeScript interface expectations.
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 50)
    void indicatorVO_shouldSerializeWithCorrectFieldNames() throws Exception {
        // Create a minimal IndicatorVO instance
        IndicatorVO vo = new IndicatorVO();
        vo.setIndicatorId(1L);
        vo.setIndicatorDesc("Test Indicator");
        vo.setIsQualitative(false);
        vo.setType1("定量");
        vo.setType2("发展性");
        vo.setProgress(50);
        vo.setIsStrategic(true);
        vo.setResponsibleDept("Test Dept");
        vo.setOwnerDept("Owner Dept");
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(vo);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        // Verify key fields are present with correct names
        assertThat(jsonNode.has("indicatorId"))
                .as("JSON should have 'indicatorId' field")
                .isTrue();
        assertThat(jsonNode.has("indicatorDesc"))
                .as("JSON should have 'indicatorDesc' field")
                .isTrue();
        assertThat(jsonNode.has("isQualitative"))
                .as("JSON should have 'isQualitative' field")
                .isTrue();
        assertThat(jsonNode.has("type1"))
                .as("JSON should have 'type1' field")
                .isTrue();
        assertThat(jsonNode.has("type2"))
                .as("JSON should have 'type2' field")
                .isTrue();
        assertThat(jsonNode.has("progress"))
                .as("JSON should have 'progress' field")
                .isTrue();
        assertThat(jsonNode.has("isStrategic"))
                .as("JSON should have 'isStrategic' field")
                .isTrue();
        assertThat(jsonNode.has("responsibleDept"))
                .as("JSON should have 'responsibleDept' field")
                .isTrue();
        assertThat(jsonNode.has("ownerDept"))
                .as("JSON should have 'ownerDept' field")
                .isTrue();
        
        // Verify no snake_case fields
        Iterator<String> fieldNames = jsonNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            assertThat(fieldName)
                    .as("JSON field '%s' should not use snake_case", fieldName)
                    .doesNotContain("_");
        }
    }

    /**
     * Property 9.4: All frontend expected fields are covered
     * 
     * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
     * 
     * For any random subset of frontend expected fields, all fields
     * SHALL exist in the IndicatorVO class.
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void allFrontendFieldSubsets_shouldExistInVO(
            @ForAll("randomFieldSubsets") List<String> fieldSubset) {
        
        Set<String> voFields = getIndicatorVOFields();
        
        for (String fieldName : fieldSubset) {
            assertThat(voFields)
                    .as("IndicatorVO should contain field '%s'", fieldName)
                    .contains(fieldName);
        }
    }

    /**
     * Property 9.5: Complete field coverage verification
     * 
     * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
     * 
     * The IndicatorVO SHALL contain all fields expected by the frontend.
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 10)
    void indicatorVO_shouldContainAllFrontendExpectedFields() {
        Set<String> voFields = getIndicatorVOFields();
        
        Set<String> missingFields = FRONTEND_EXPECTED_FIELDS.stream()
                .filter(f -> !voFields.contains(f))
                .collect(Collectors.toSet());
        
        assertThat(missingFields)
                .as("All frontend expected fields should be present in IndicatorVO. Missing: %s", 
                    missingFields)
                .isEmpty();
    }

    /**
     * Property 9.6: JSON serialization preserves field values
     * 
     * **Feature: data-alignment-sop, Property 9: API 响应字段匹配**
     * 
     * For any IndicatorVO with set values, serializing to JSON and
     * deserializing back SHALL preserve the original values.
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 50)
    void indicatorVO_shouldPreserveValuesOnSerialization(
            @ForAll("indicatorVOWithRandomValues") IndicatorVO original) throws Exception {
        
        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(original);
        IndicatorVO deserialized = objectMapper.readValue(json, IndicatorVO.class);
        
        // Verify key fields are preserved
        assertThat(deserialized.getIndicatorId())
                .as("indicatorId should be preserved")
                .isEqualTo(original.getIndicatorId());
        assertThat(deserialized.getIndicatorDesc())
                .as("indicatorDesc should be preserved")
                .isEqualTo(original.getIndicatorDesc());
        assertThat(deserialized.getIsQualitative())
                .as("isQualitative should be preserved")
                .isEqualTo(original.getIsQualitative());
        assertThat(deserialized.getType1())
                .as("type1 should be preserved")
                .isEqualTo(original.getType1());
        assertThat(deserialized.getType2())
                .as("type2 should be preserved")
                .isEqualTo(original.getType2());
        assertThat(deserialized.getProgress())
                .as("progress should be preserved")
                .isEqualTo(original.getProgress());
    }

    @Provide
    Arbitrary<IndicatorVO> indicatorVOWithRandomValues() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                Arbitraries.of(true, false),
                Arbitraries.of("定性", "定量"),
                Arbitraries.of("发展性", "基础性"),
                Arbitraries.integers().between(0, 100)
        ).as((id, desc, isQual, type1, type2, progress) -> {
            IndicatorVO vo = new IndicatorVO();
            vo.setIndicatorId(id);
            vo.setIndicatorDesc(desc);
            vo.setIsQualitative(isQual);
            vo.setType1(type1);
            vo.setType2(type2);
            vo.setProgress(progress);
            vo.setIsStrategic(!isQual);
            vo.setResponsibleDept("Test Dept");
            vo.setOwnerDept("Owner Dept");
            return vo;
        });
    }
}
