package com.sism.enums;

/**
 * Indicator level enumeration
 * Defines the hierarchical levels of indicators
 */
public enum IndicatorLevel {
    /**
     * Primary level indicator (Level 1)
     * Strategic to functional level
     */
    PRIMARY,
    
    /**
     * Secondary level indicator (Level 2)
     * Functional to college level
     */
    SECONDARY,
    
    /**
     * @deprecated Use PRIMARY instead
     * Strategic to functional level indicator (Level 1)
     */
    @Deprecated
    STRAT_TO_FUNC,
    
    /**
     * @deprecated Use SECONDARY instead
     * Functional to college level indicator (Level 2)
     */
    @Deprecated
    FUNC_TO_COLLEGE
}
