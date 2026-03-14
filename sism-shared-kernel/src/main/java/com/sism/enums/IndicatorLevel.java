package com.sism.enums;

/**
 * Indicator level enumeration for the filling distribution workflow system
 * Defines the hierarchical levels of indicators in the two-tier workflow
 * 
 * Workflow hierarchy:
 * - FIRST: Strategic Development Department -> Functional Department
 * - SECOND: Functional Department -> Secondary College
 */
public enum IndicatorLevel {
    /**
     * First-level indicator (一级指标)
     * Distributed from Strategic Development Department to Functional Department
     * Equivalent to PRIMARY level
     */
    FIRST(1),
    
    /**
     * Second-level indicator (二级指标)
     * Distributed from Functional Department to Secondary College
     * Equivalent to SECONDARY level
     */
    SECOND(2),
    
    /**
     * Primary level indicator (Level 1)
     * Strategic to functional level
     * @deprecated Use FIRST instead for workflow system consistency
     */
    @Deprecated
    PRIMARY(1),
    
    /**
     * Secondary level indicator (Level 2)
     * Functional to college level
     * @deprecated Use SECOND instead for workflow system consistency
     */
    @Deprecated
    SECONDARY(2),
    
    /**
     * @deprecated Use FIRST instead
     * Strategic to functional level indicator (Level 1)
     */
    @Deprecated
    STRAT_TO_FUNC(1),
    
    /**
     * @deprecated Use SECOND instead
     * Functional to college level indicator (Level 2)
     */
    @Deprecated
    FUNC_TO_COLLEGE(2);
    
    private final int level;
    
    IndicatorLevel(int level) {
        this.level = level;
    }
    
    /**
     * Get the numeric level value
     * @return 1 for first-level, 2 for second-level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Get IndicatorLevel from numeric value
     * @param level numeric level (1 or 2)
     * @return corresponding IndicatorLevel
     * @throws IllegalArgumentException if level is not 1 or 2
     */
    public static IndicatorLevel fromLevel(int level) {
        return switch (level) {
            case 1 -> FIRST;
            case 2 -> SECOND;
            default -> throw new IllegalArgumentException("Invalid indicator level: " + level + ". Must be 1 or 2.");
        };
    }
    
    /**
     * Check if this is a first-level indicator
     * @return true if this is FIRST level
     */
    public boolean isFirst() {
        return this == FIRST || this == PRIMARY || this == STRAT_TO_FUNC;
    }
    
    /**
     * Check if this is a second-level indicator
     * @return true if this is SECOND level
     */
    public boolean isSecond() {
        return this == SECOND || this == SECONDARY || this == FUNC_TO_COLLEGE;
    }
}
