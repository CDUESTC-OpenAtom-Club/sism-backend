package com.sism.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Plan Approval DTOs
 * Requirements: 2.4
 */
class PlanApprovalDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testPlanApprovalRequest_Valid() {
        PlanApprovalRequest request = new PlanApprovalRequest();
        request.setComment("Approved");

        Set<ConstraintViolation<PlanApprovalRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testPlanApprovalRequest_ValidWithoutComment() {
        PlanApprovalRequest request = new PlanApprovalRequest();

        Set<ConstraintViolation<PlanApprovalRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Comment is optional, should have no violations");
    }

    @Test
    void testPlanApprovalRequest_CommentTooLong() {
        PlanApprovalRequest request = new PlanApprovalRequest();
        request.setComment("A".repeat(501)); // 501 characters

        Set<ConstraintViolation<PlanApprovalRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size(), "Should have one violation for comment too long");
        
        ConstraintViolation<PlanApprovalRequest> violation = violations.iterator().next();
        assertEquals("comment", violation.getPropertyPath().toString());
        assertEquals("Comment must not exceed 500 characters", violation.getMessage());
    }

    @Test
    void testPlanRejectionRequest_Valid() {
        PlanRejectionRequest request = new PlanRejectionRequest();
        request.setReason("Does not meet requirements");

        Set<ConstraintViolation<PlanRejectionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testPlanRejectionRequest_MissingReason() {
        PlanRejectionRequest request = new PlanRejectionRequest();

        Set<ConstraintViolation<PlanRejectionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size(), "Should have one violation for missing reason");
        
        ConstraintViolation<PlanRejectionRequest> violation = violations.iterator().next();
        assertEquals("reason", violation.getPropertyPath().toString());
        assertEquals("Reason is required for rejection", violation.getMessage());
    }

    @Test
    void testPlanRejectionRequest_EmptyReason() {
        PlanRejectionRequest request = new PlanRejectionRequest();
        request.setReason("");

        Set<ConstraintViolation<PlanRejectionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size(), "Should have one violation for empty reason");
        
        ConstraintViolation<PlanRejectionRequest> violation = violations.iterator().next();
        assertEquals("reason", violation.getPropertyPath().toString());
        assertEquals("Reason is required for rejection", violation.getMessage());
    }

    @Test
    void testPlanRejectionRequest_ReasonTooLong() {
        PlanRejectionRequest request = new PlanRejectionRequest();
        request.setReason("A".repeat(501)); // 501 characters

        Set<ConstraintViolation<PlanRejectionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size(), "Should have one violation for reason too long");
        
        ConstraintViolation<PlanRejectionRequest> violation = violations.iterator().next();
        assertEquals("reason", violation.getPropertyPath().toString());
        assertEquals("Reason must not exceed 500 characters", violation.getMessage());
    }
}
