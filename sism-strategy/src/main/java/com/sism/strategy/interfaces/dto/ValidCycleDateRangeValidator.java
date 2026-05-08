package com.sism.strategy.interfaces.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidCycleDateRangeValidator implements ConstraintValidator<ValidCycleDateRange, CreateCycleRequest> {

    @Override
    public boolean isValid(CreateCycleRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null) {
            return true;
        }
        return !request.getEndDate().isBefore(request.getStartDate());
    }
}
