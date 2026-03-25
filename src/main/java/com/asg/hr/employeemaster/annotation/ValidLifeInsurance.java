package com.asg.hr.employeemaster.annotation;

import com.asg.hr.employeemaster.validator.LifeInsuranceValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LifeInsuranceValidator.class)
@Documented
public @interface ValidLifeInsurance {

    String message() default "Insurance nominee details are required when life insurance is Y";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}