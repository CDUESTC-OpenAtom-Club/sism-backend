package com.sism.property;

import com.sism.config.EnvConfigValidator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.Size;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Environment Configuration Validator
 * 
 * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
 * 
 * For any subset of required environment variables missing, the system should 
 * refuse to start and report all missing variables.
 * 
 * **Validates: Requirements 1.1.1, 1.1.2, 1.1.4**
 */
public class EnvConfigPropertyTest {

    private static final List<String> REQUIRED_VARS = EnvConfigValidator.getRequiredVars();

    // ==================== Generators ====================

    /**
     * Generator for non-empty subsets of required environment variables
     */
    @Provide
    Arbitrary<Set<String>> nonEmptySubsetsOfRequiredVars() {
        return Arbitraries.of(REQUIRED_VARS)
                .set()
                .ofMinSize(1)
                .ofMaxSize(REQUIRED_VARS.size());
    }

    /**
     * Generator for all possible subsets of required environment variables (including empty)
     */
    @Provide
    Arbitrary<Set<String>> allSubsetsOfRequiredVars() {
        return Arbitraries.of(REQUIRED_VARS)
                .set()
                .ofMinSize(0)
                .ofMaxSize(REQUIRED_VARS.size());
    }

    /**
     * Generator for random environment variable names (not in required list)
     */
    @Provide
    Arbitrary<String> randomEnvVarNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(String::toUpperCase)
                .filter(s -> !REQUIRED_VARS.contains(s));
    }

    /**
     * Generator for valid environment variable values
     */
    @Provide
    Arbitrary<String> validEnvValues() {
        return Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(8).ofMaxLength(64),
                Arbitraries.strings().ascii().ofMinLength(8).ofMaxLength(64)
                        .filter(s -> !s.isBlank())
        );
    }

    // ==================== Property Tests ====================

    /**
     * Property P1.1: Error message contains all missing variables
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * For any non-empty subset of required environment variables that are missing,
     * the error message SHALL contain all of those missing variable names.
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 100)
    void errorMessage_shouldContainAllMissingVariables(
            @ForAll("nonEmptySubsetsOfRequiredVars") Set<String> missingVars) {
        
        // Build error message for the missing variables
        List<String> missingList = new ArrayList<>(missingVars);
        String errorMessage = EnvConfigValidator.buildErrorMessage(missingList);
        
        // Assert: Error message should contain all missing variable names
        for (String var : missingVars) {
            assertThat(errorMessage)
                    .as("Error message should contain missing variable: %s", var)
                    .contains(var);
        }
    }

    /**
     * Property P1.2: Error message does not contain configured variables
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * For any subset of required environment variables that are missing,
     * the error message SHALL NOT list variables that are NOT in the missing set
     * as missing (they should only appear in descriptions, not in the missing list).
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 100)
    void errorMessage_shouldOnlyListMissingVariables(
            @ForAll("nonEmptySubsetsOfRequiredVars") Set<String> missingVars) {
        
        // Build error message for the missing variables
        List<String> missingList = new ArrayList<>(missingVars);
        String errorMessage = EnvConfigValidator.buildErrorMessage(missingList);
        
        // Get the "Missing required environment variables: [...]" line
        String firstLine = errorMessage.split("\n")[0];
        
        // Assert: First line should only contain the missing variables
        for (String var : REQUIRED_VARS) {
            if (!missingVars.contains(var)) {
                assertThat(firstLine)
                        .as("First line should not list configured variable: %s", var)
                        .doesNotContain(var);
            }
        }
    }

    /**
     * Property P1.3: Required variables list is complete
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * The required variables list SHALL contain exactly the four specified variables:
     * JWT_SECRET, DB_URL, DB_USERNAME, DB_PASSWORD.
     * 
     * **Validates: Requirements 1.1.1, 1.1.2**
     */
    @Property(tries = 1)
    void requiredVarsList_shouldContainAllSpecifiedVariables() {
        List<String> expectedVars = List.of(
                "JWT_SECRET",
                "DB_URL",
                "DB_USERNAME",
                "DB_PASSWORD"
        );
        
        assertThat(REQUIRED_VARS)
                .as("Required variables list should contain exactly the specified variables")
                .containsExactlyInAnyOrderElementsOf(expectedVars);
    }

    /**
     * Property P1.4: Required variables list is immutable
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * The required variables list SHALL be immutable to prevent runtime modification.
     * 
     * **Validates: Requirements 1.1.1**
     */
    @Property(tries = 10)
    void requiredVarsList_shouldBeImmutable(
            @ForAll("randomEnvVarNames") String randomVar) {
        
        List<String> vars = EnvConfigValidator.getRequiredVars();
        int originalSize = vars.size();
        
        // Attempt to modify the list should throw an exception
        try {
            vars.add(randomVar);
            // If we get here, the list is mutable - this is a failure
            assertThat(false)
                    .as("Required variables list should be immutable")
                    .isTrue();
        } catch (UnsupportedOperationException e) {
            // Expected behavior - list is immutable
            assertThat(vars.size())
                    .as("List size should remain unchanged")
                    .isEqualTo(originalSize);
        }
    }

    /**
     * Property P1.5: Error message format is consistent
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * For any non-empty subset of missing variables, the error message SHALL:
     * - Start with "Missing required environment variables:"
     * - Contain the list of missing variables
     * - Provide configuration instructions
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 50)
    void errorMessage_shouldHaveConsistentFormat(
            @ForAll("nonEmptySubsetsOfRequiredVars") Set<String> missingVars) {
        
        List<String> missingList = new ArrayList<>(missingVars);
        String errorMessage = EnvConfigValidator.buildErrorMessage(missingList);
        
        // Assert: Error message should have the expected format
        assertThat(errorMessage)
                .as("Error message should start with the expected prefix")
                .startsWith("Missing required environment variables:");
        
        assertThat(errorMessage)
                .as("Error message should contain configuration instructions")
                .contains("Please configure the following environment variables");
        
        assertThat(errorMessage)
                .as("Error message should reference .env.example")
                .contains(".env.example");
    }

    /**
     * Property P1.6: Each missing variable has a description
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * For any missing variable in the error message, there SHALL be a description
     * explaining what the variable is used for.
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 50)
    void errorMessage_shouldProvideDescriptionForEachMissingVariable(
            @ForAll("nonEmptySubsetsOfRequiredVars") Set<String> missingVars) {
        
        List<String> missingList = new ArrayList<>(missingVars);
        String errorMessage = EnvConfigValidator.buildErrorMessage(missingList);
        
        // Assert: Each missing variable should have a description line
        for (String var : missingVars) {
            String expectedPattern = "- " + var + ":";
            assertThat(errorMessage)
                    .as("Error message should contain description for: %s", var)
                    .contains(expectedPattern);
        }
    }

    /**
     * Property P1.7: Empty missing list produces no error
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * When all required variables are configured (empty missing list),
     * the buildErrorMessage method should still produce a valid message
     * (though it would not be used in practice).
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 1)
    void errorMessage_withEmptyList_shouldStillBeValid() {
        List<String> emptyList = Collections.emptyList();
        String errorMessage = EnvConfigValidator.buildErrorMessage(emptyList);
        
        // Assert: Error message should still be well-formed
        assertThat(errorMessage)
                .as("Error message should not be null")
                .isNotNull();
        
        assertThat(errorMessage)
                .as("Error message should contain the empty list indicator")
                .contains("[]");
    }

    /**
     * Property P1.8: Missing variables detection is deterministic
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * For the same set of missing variables, the error message SHALL be identical
     * across multiple invocations (deterministic behavior).
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 50)
    void errorMessage_shouldBeDeterministic(
            @ForAll("nonEmptySubsetsOfRequiredVars") Set<String> missingVars) {
        
        List<String> missingList = new ArrayList<>(missingVars);
        // Sort to ensure consistent ordering
        Collections.sort(missingList);
        
        String errorMessage1 = EnvConfigValidator.buildErrorMessage(missingList);
        String errorMessage2 = EnvConfigValidator.buildErrorMessage(missingList);
        
        assertThat(errorMessage1)
                .as("Error message should be deterministic")
                .isEqualTo(errorMessage2);
    }

    /**
     * Property P1.9: All required variables have meaningful descriptions
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * Each required variable SHALL have a non-empty, meaningful description
     * in the error message.
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 1)
    void allRequiredVariables_shouldHaveMeaningfulDescriptions() {
        String errorMessage = EnvConfigValidator.buildErrorMessage(new ArrayList<>(REQUIRED_VARS));
        
        // Check that each variable has a description longer than just the variable name
        for (String var : REQUIRED_VARS) {
            // Find the line containing this variable's description
            String[] lines = errorMessage.split("\n");
            boolean foundDescription = false;
            
            for (String line : lines) {
                if (line.contains("- " + var + ":")) {
                    // Extract the description part (after the colon)
                    int colonIndex = line.indexOf(var + ":");
                    if (colonIndex >= 0) {
                        String description = line.substring(colonIndex + var.length() + 1).trim();
                        assertThat(description)
                                .as("Description for %s should be meaningful", var)
                                .isNotEmpty()
                                .hasSizeGreaterThan(10); // At least 10 characters
                        foundDescription = true;
                    }
                }
            }
            
            assertThat(foundDescription)
                    .as("Should find description for variable: %s", var)
                    .isTrue();
        }
    }

    /**
     * Property P1.10: Subset relationship is preserved in error reporting
     * 
     * **Feature: sism-enterprise-optimization, Property P1: 环境变量配置完整性**
     * 
     * If subset A ⊂ subset B of missing variables, then the error message for B
     * SHALL contain all variables mentioned in the error message for A.
     * 
     * **Validates: Requirements 1.1.4**
     */
    @Property(tries = 50)
    void errorMessage_shouldPreserveSubsetRelationship(
            @ForAll("nonEmptySubsetsOfRequiredVars") Set<String> subsetB) {
        
        if (subsetB.size() <= 1) {
            return; // Need at least 2 elements to create a proper subset
        }
        
        // Create a proper subset A of B
        List<String> listB = new ArrayList<>(subsetB);
        List<String> listA = listB.subList(0, listB.size() - 1);
        
        String errorMessageA = EnvConfigValidator.buildErrorMessage(listA);
        String errorMessageB = EnvConfigValidator.buildErrorMessage(listB);
        
        // All variables in A should also be in B's error message
        for (String var : listA) {
            assertThat(errorMessageB)
                    .as("Error message for superset should contain variable from subset: %s", var)
                    .contains(var);
        }
    }
}
