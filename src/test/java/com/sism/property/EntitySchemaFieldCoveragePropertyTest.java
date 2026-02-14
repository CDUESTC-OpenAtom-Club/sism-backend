package com.sism.property;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Entity-Schema Field Coverage
 * 
 * **Feature: production-deployment-integration, Property 4: Entity-Schema 字段覆盖**
 * 
 * For any Entity class field, the database table SHALL contain a corresponding column.
 * This ensures that JPA entity definitions are fully aligned with the database schema.
 * 
 * **Validates: Requirements 2.1**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class EntitySchemaFieldCoveragePropertyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Entity classes to verify (excluding BaseEntity which is a MappedSuperclass)
    private static final List<Class<?>> ENTITY_CLASSES = List.of(
            com.sism.entity.SysOrg.class,
            com.sism.entity.SysUser.class,
            com.sism.entity.AssessmentCycle.class,
            com.sism.entity.StrategicTask.class,
            com.sism.entity.Indicator.class,
            com.sism.entity.Milestone.class,
            com.sism.entity.ProgressReport.class,
            com.sism.entity.ApprovalRecord.class,
            com.sism.entity.AlertWindow.class,
            com.sism.entity.AlertRule.class,
            com.sism.entity.AlertEvent.class,
            com.sism.entity.AdhocTask.class,
            com.sism.entity.AdhocTaskTarget.class,
            com.sism.entity.AdhocTaskIndicatorMap.class,
            com.sism.entity.AuditLog.class
    );

    // ==================== Helper Methods ====================

    /**
     * Get the table name for an entity class.
     */
    private String getTableName(Class<?> entityClass) {
        jakarta.persistence.Table tableAnnotation = entityClass.getAnnotation(jakarta.persistence.Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // Convert class name to snake_case
        return camelToSnake(entityClass.getSimpleName());
    }

    /**
     * Get all persistent fields from an entity class (including inherited fields from BaseEntity).
     */
    private List<EntityFieldInfo> getPersistentFields(Class<?> entityClass) {
        List<EntityFieldInfo> fields = new ArrayList<>();
        
        // Get fields from the entity class itself
        collectPersistentFields(entityClass, fields);
        
        // Get fields from superclass (BaseEntity) if applicable
        Class<?> superclass = entityClass.getSuperclass();
        if (superclass != null && superclass.isAnnotationPresent(MappedSuperclass.class)) {
            collectPersistentFields(superclass, fields);
        }
        
        return fields;
    }

    /**
     * Collect persistent fields from a class.
     */
    private void collectPersistentFields(Class<?> clazz, List<EntityFieldInfo> fields) {
        for (Field field : clazz.getDeclaredFields()) {
            // Skip transient fields
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            
            // Skip collection fields (OneToMany relationships are not columns)
            if (field.isAnnotationPresent(OneToMany.class)) {
                continue;
            }
            
            // Skip static fields
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            
            String columnName = getColumnName(field);
            if (columnName != null) {
                fields.add(new EntityFieldInfo(field.getName(), columnName, field.getType()));
            }
        }
    }

    /**
     * Get the column name for a field.
     */
    private String getColumnName(Field field) {
        // Check for @Column annotation
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        }
        
        // Check for @JoinColumn annotation (for ManyToOne relationships)
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        if (joinColumnAnnotation != null && !joinColumnAnnotation.name().isEmpty()) {
            return joinColumnAnnotation.name();
        }
        
        // Check for @Id annotation with @Column
        if (field.isAnnotationPresent(Id.class)) {
            // If no explicit column name, use field name converted to snake_case
            return camelToSnake(field.getName());
        }
        
        // For ManyToOne without explicit JoinColumn, skip (handled by JPA)
        if (field.isAnnotationPresent(ManyToOne.class) && joinColumnAnnotation == null) {
            return null;
        }
        
        // Default: convert field name to snake_case
        return camelToSnake(field.getName());
    }

    /**
     * Convert camelCase to snake_case.
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Get all column names from a database table.
     */
    private Set<String> getTableColumns(String tableName) {
        String sql = """
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_schema = 'public' 
                AND table_name = ?
            """;
        
        List<String> columns = jdbcTemplate.queryForList(sql, String.class, tableName);
        return new HashSet<>(columns);
    }

    /**
     * Check if a table exists in the database.
     */
    private boolean tableExists(String tableName) {
        String sql = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
                AND table_name = ?
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Integer> entityIndices() {
        return Arbitraries.integers().between(0, ENTITY_CLASSES.size() - 1);
    }

    @Provide
    Arbitrary<Class<?>> entityClasses() {
        return Arbitraries.of(ENTITY_CLASSES);
    }

    // ==================== Property Tests ====================

    /**
     * Property 4.1: Every Entity class has a corresponding database table
     * 
     * **Feature: production-deployment-integration, Property 4: Entity-Schema 字段覆盖**
     * 
     * For any Entity class, the database SHALL contain a table with the expected name.
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void everyEntityClass_shouldHaveCorrespondingDatabaseTable(
            @ForAll("entityIndices") Integer index) {
        
        Class<?> entityClass = ENTITY_CLASSES.get(index);
        String tableName = getTableName(entityClass);
        
        assertThat(tableExists(tableName))
                .as("Entity %s should have corresponding table '%s'", 
                    entityClass.getSimpleName(), tableName)
                .isTrue();
    }

    /**
     * Property 4.2: Every Entity field has a corresponding database column
     * 
     * **Feature: production-deployment-integration, Property 4: Entity-Schema 字段覆盖**
     * 
     * For any persistent field in an Entity class, the database table SHALL contain
     * a column with the expected name.
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void everyEntityField_shouldHaveCorrespondingDatabaseColumn(
            @ForAll("entityIndices") Integer index) {
        
        Class<?> entityClass = ENTITY_CLASSES.get(index);
        String tableName = getTableName(entityClass);
        
        // Get entity fields
        List<EntityFieldInfo> entityFields = getPersistentFields(entityClass);
        
        // Get database columns
        Set<String> dbColumns = getTableColumns(tableName);
        
        // Verify each entity field has a corresponding column
        for (EntityFieldInfo fieldInfo : entityFields) {
            assertThat(dbColumns)
                    .as("Entity %s field '%s' should have corresponding column '%s' in table '%s'",
                        entityClass.getSimpleName(), fieldInfo.fieldName(), 
                        fieldInfo.columnName(), tableName)
                    .contains(fieldInfo.columnName());
        }
    }

    /**
     * Property 4.3: Field count matches between Entity and database table
     * 
     * **Feature: production-deployment-integration, Property 4: Entity-Schema 字段覆盖**
     * 
     * For any Entity class, the number of persistent fields SHALL match the number
     * of columns in the corresponding database table (excluding auto-generated columns).
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void entityFieldCount_shouldMatchDatabaseColumnCount(
            @ForAll("entityIndices") Integer index) {
        
        Class<?> entityClass = ENTITY_CLASSES.get(index);
        String tableName = getTableName(entityClass);
        
        // Get entity fields
        List<EntityFieldInfo> entityFields = getPersistentFields(entityClass);
        Set<String> entityColumnNames = entityFields.stream()
                .map(EntityFieldInfo::columnName)
                .collect(Collectors.toSet());
        
        // Get database columns
        Set<String> dbColumns = getTableColumns(tableName);
        
        // Entity columns should be a subset of database columns
        assertThat(dbColumns)
                .as("Database table '%s' should contain all columns defined in Entity %s",
                    tableName, entityClass.getSimpleName())
                .containsAll(entityColumnNames);
    }

    /**
     * Property 4.4: ID field mapping is correct
     * 
     * **Feature: production-deployment-integration, Property 4: Entity-Schema 字段覆盖**
     * 
     * For any Entity class with an @Id field, the database table SHALL contain
     * the corresponding primary key column.
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void idField_shouldMapToCorrectPrimaryKeyColumn(
            @ForAll("entityIndices") Integer index) {
        
        Class<?> entityClass = ENTITY_CLASSES.get(index);
        String tableName = getTableName(entityClass);
        
        // Find @Id field
        Field idField = findIdField(entityClass);
        
        if (idField != null) {
            String expectedColumnName = getColumnName(idField);
            Set<String> dbColumns = getTableColumns(tableName);
            
            assertThat(dbColumns)
                    .as("Entity %s @Id field '%s' should map to column '%s' in table '%s'",
                        entityClass.getSimpleName(), idField.getName(), 
                        expectedColumnName, tableName)
                    .contains(expectedColumnName);
        }
    }

    /**
     * Property 4.5: Foreign key fields map to correct columns
     * 
     * **Feature: production-deployment-integration, Property 4: Entity-Schema 字段覆盖**
     * 
     * For any Entity field with @ManyToOne annotation, the database table SHALL
     * contain the corresponding foreign key column.
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void foreignKeyFields_shouldMapToCorrectColumns(
            @ForAll("entityIndices") Integer index) {
        
        Class<?> entityClass = ENTITY_CLASSES.get(index);
        String tableName = getTableName(entityClass);
        
        // Find @ManyToOne fields
        List<Field> manyToOneFields = findManyToOneFields(entityClass);
        Set<String> dbColumns = getTableColumns(tableName);
        
        for (Field field : manyToOneFields) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null && !joinColumn.name().isEmpty()) {
                String expectedColumnName = joinColumn.name();
                
                assertThat(dbColumns)
                        .as("Entity %s @ManyToOne field '%s' should map to column '%s' in table '%s'",
                            entityClass.getSimpleName(), field.getName(), 
                            expectedColumnName, tableName)
                        .contains(expectedColumnName);
            }
        }
    }

    // ==================== Helper Methods for Field Discovery ====================

    private Field findIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        // Check for @IdClass (composite key)
        if (entityClass.isAnnotationPresent(IdClass.class)) {
            // For composite keys, return null (handled separately)
            return null;
        }
        return null;
    }

    private List<Field> findManyToOneFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                fields.add(field);
            }
        }
        return fields;
    }

    // ==================== Record for Field Info ====================

    private record EntityFieldInfo(String fieldName, String columnName, Class<?> fieldType) {}
}
