package com.sism.property;

import com.sism.enums.*;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Enum Value Consistency
 * 
 * **Feature: production-deployment-integration, Property 7: 枚举值一致性**
 * 
 * For any enum type field, the frontend enum values SHALL be consistent with
 * the backend Enum definition, and the backend Enum values SHALL be consistent
 * with the PostgreSQL enum type definition.
 * 
 * **Validates: Requirements 2.4**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class EnumValueConsistencyPropertyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== Enum Mapping Configuration ====================

    /**
     * Mapping of Java enum classes to their PostgreSQL enum type names.
     */
    private static final Map<Class<? extends Enum<?>>, String> ENUM_TYPE_MAPPING = Map.ofEntries(
            Map.entry(OrgType.class, "org_type"),
            Map.entry(TaskType.class, "task_type"),
            Map.entry(IndicatorLevel.class, "indicator_level"),
            Map.entry(IndicatorStatus.class, "indicator_status"),
            Map.entry(MilestoneStatus.class, "milestone_status"),
            Map.entry(ReportStatus.class, "report_status"),
            Map.entry(ApprovalAction.class, "approval_action"),
            Map.entry(AlertSeverity.class, "alert_severity"),
            Map.entry(AlertStatus.class, "alert_status"),
            Map.entry(AdhocScopeType.class, "adhoc_scope_type"),
            Map.entry(AdhocTaskStatus.class, "adhoc_task_status"),
            Map.entry(AuditAction.class, "audit_action"),
            Map.entry(AuditEntityType.class, "audit_entity_type")
    );

    private static final List<Class<? extends Enum<?>>> ENUM_CLASSES = new ArrayList<>(ENUM_TYPE_MAPPING.keySet());

    // ==================== Helper Methods ====================

    /**
     * Get all enum values from a PostgreSQL enum type.
     */
    private Set<String> getPostgresEnumValues(String enumTypeName) {
        String sql = """
            SELECT enumlabel 
            FROM pg_enum e
            JOIN pg_type t ON e.enumtypid = t.oid
            WHERE t.typname = ?
            ORDER BY e.enumsortorder
            """;
        
        List<String> values = jdbcTemplate.queryForList(sql, String.class, enumTypeName);
        return new HashSet<>(values);
    }

    /**
     * Get all enum values from a Java enum class.
     */
    private Set<String> getJavaEnumValues(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    /**
     * Check if a PostgreSQL enum type exists.
     */
    private boolean postgresEnumExists(String enumTypeName) {
        String sql = """
            SELECT COUNT(*) 
            FROM pg_type 
            WHERE typname = ? AND typtype = 'e'
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, enumTypeName);
        return count != null && count > 0;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Integer> enumIndices() {
        return Arbitraries.integers().between(0, ENUM_CLASSES.size() - 1);
    }

    @Provide
    Arbitrary<Class<? extends Enum<?>>> enumClasses() {
        return Arbitraries.of(ENUM_CLASSES);
    }

    // ==================== Property Tests ====================

    /**
     * Property 7.1: Every Java enum has a corresponding PostgreSQL enum type
     * 
     * **Feature: production-deployment-integration, Property 7: 枚举值一致性**
     * 
     * For any Java enum class used in entities, the database SHALL contain
     * a corresponding PostgreSQL enum type.
     * 
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void everyJavaEnum_shouldHaveCorrespondingPostgresEnumType(
            @ForAll("enumIndices") Integer index) {
        
        Class<? extends Enum<?>> enumClass = ENUM_CLASSES.get(index);
        String postgresEnumName = ENUM_TYPE_MAPPING.get(enumClass);
        
        assertThat(postgresEnumExists(postgresEnumName))
                .as("Java enum %s should have corresponding PostgreSQL enum type '%s'",
                    enumClass.getSimpleName(), postgresEnumName)
                .isTrue();
    }

    /**
     * Property 7.2: PostgreSQL enum values are a subset of Java enum values
     * 
     * **Feature: production-deployment-integration, Property 7: 枚举值一致性**
     * 
     * For any PostgreSQL enum type, all its values SHALL exist in the
     * corresponding Java enum class. This ensures the database can store
     * any value that the application might receive.
     * 
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void postgresEnumValues_shouldBeSubsetOfJavaEnumValues(
            @ForAll("enumIndices") Integer index) {
        
        Class<? extends Enum<?>> enumClass = ENUM_CLASSES.get(index);
        String postgresEnumName = ENUM_TYPE_MAPPING.get(enumClass);
        
        Set<String> javaValues = getJavaEnumValues(enumClass);
        Set<String> postgresValues = getPostgresEnumValues(postgresEnumName);
        
        // Every PostgreSQL enum value should exist in Java enum
        for (String pgValue : postgresValues) {
            assertThat(javaValues)
                    .as("PostgreSQL enum '%s' value '%s' should exist in Java enum %s. " +
                        "Java values: %s, PostgreSQL values: %s",
                        postgresEnumName, pgValue, enumClass.getSimpleName(),
                        javaValues, postgresValues)
                    .contains(pgValue);
        }
    }

    /**
     * Property 7.3: Java enum values used in database operations exist in PostgreSQL
     * 
     * **Feature: production-deployment-integration, Property 7: 枚举值一致性**
     * 
     * For any Java enum value that might be persisted to the database,
     * the PostgreSQL enum type SHALL contain that value.
     * 
     * Note: This property checks that Java enum values are a subset of PostgreSQL
     * enum values, ensuring JPA can persist any Java enum value.
     * 
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void javaEnumValues_shouldExistInPostgresEnum(
            @ForAll("enumIndices") Integer index) {
        
        Class<? extends Enum<?>> enumClass = ENUM_CLASSES.get(index);
        String postgresEnumName = ENUM_TYPE_MAPPING.get(enumClass);
        
        Set<String> javaValues = getJavaEnumValues(enumClass);
        Set<String> postgresValues = getPostgresEnumValues(postgresEnumName);
        
        // Every Java enum value should exist in PostgreSQL enum
        for (String javaValue : javaValues) {
            assertThat(postgresValues)
                    .as("Java enum %s value '%s' should exist in PostgreSQL enum '%s'. " +
                        "Java values: %s, PostgreSQL values: %s",
                        enumClass.getSimpleName(), javaValue, postgresEnumName,
                        javaValues, postgresValues)
                    .contains(javaValue);
        }
    }

    /**
     * Property 7.4: Enum value sets are exactly equal
     * 
     * **Feature: production-deployment-integration, Property 7: 枚举值一致性**
     * 
     * For any enum type, the Java enum values SHALL exactly match the
     * PostgreSQL enum values (bidirectional consistency).
     * 
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void enumValueSets_shouldBeExactlyEqual(
            @ForAll("enumIndices") Integer index) {
        
        Class<? extends Enum<?>> enumClass = ENUM_CLASSES.get(index);
        String postgresEnumName = ENUM_TYPE_MAPPING.get(enumClass);
        
        Set<String> javaValues = getJavaEnumValues(enumClass);
        Set<String> postgresValues = getPostgresEnumValues(postgresEnumName);
        
        assertThat(javaValues)
                .as("Java enum %s values should exactly match PostgreSQL enum '%s' values. " +
                    "Java values: %s, PostgreSQL values: %s",
                    enumClass.getSimpleName(), postgresEnumName,
                    javaValues, postgresValues)
                .isEqualTo(postgresValues);
    }

    /**
     * Property 7.5: Enum value count matches
     * 
     * **Feature: production-deployment-integration, Property 7: 枚举值一致性**
     * 
     * For any enum type, the number of Java enum values SHALL equal
     * the number of PostgreSQL enum values.
     * 
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void enumValueCount_shouldMatch(
            @ForAll("enumIndices") Integer index) {
        
        Class<? extends Enum<?>> enumClass = ENUM_CLASSES.get(index);
        String postgresEnumName = ENUM_TYPE_MAPPING.get(enumClass);
        
        Set<String> javaValues = getJavaEnumValues(enumClass);
        Set<String> postgresValues = getPostgresEnumValues(postgresEnumName);
        
        assertThat(javaValues.size())
                .as("Java enum %s should have same number of values as PostgreSQL enum '%s'. " +
                    "Java count: %d (%s), PostgreSQL count: %d (%s)",
                    enumClass.getSimpleName(), postgresEnumName,
                    javaValues.size(), javaValues,
                    postgresValues.size(), postgresValues)
                .isEqualTo(postgresValues.size());
    }
}
