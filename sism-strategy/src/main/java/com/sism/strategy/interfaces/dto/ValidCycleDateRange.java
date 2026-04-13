package com.sism.strategy.interfaces.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ValidCycleDateRangeValidator.class)
public @interface ValidCycleDateRange {

    String message() default "结束日期必须晚于或等于开始日期";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
