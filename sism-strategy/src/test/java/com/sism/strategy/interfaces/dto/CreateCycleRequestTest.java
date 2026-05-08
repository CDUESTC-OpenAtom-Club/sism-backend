package com.sism.strategy.interfaces.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCycleRequestTest {

    private final Validator validator;

    CreateCycleRequestTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        CreateCycleRequest request = new CreateCycleRequest();
        request.setName("2026考核周期");
        request.setYear(2026);
        request.setStartDate(LocalDate.of(2026, 12, 31));
        request.setEndDate(LocalDate.of(2026, 1, 1));

        Set<String> messages = validator.validate(request).stream()
                .map(constraintViolation -> constraintViolation.getMessage())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(messages.contains("结束日期必须晚于或等于开始日期"));
    }
}
