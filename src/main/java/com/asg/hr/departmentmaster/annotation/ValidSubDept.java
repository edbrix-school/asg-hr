package com.asg.hr.departmentmaster.annotation;

import com.asg.hr.departmentmaster.validator.SubDeptValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SubDeptValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSubDept {
    String message() default "parentDeptPoid is mandatory when subdeptYN is 'Y'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}