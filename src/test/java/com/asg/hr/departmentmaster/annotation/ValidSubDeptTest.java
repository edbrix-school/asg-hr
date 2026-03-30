package com.asg.hr.departmentmaster.annotation;

import jakarta.validation.Payload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

class ValidSubDeptTest {

    @Test
    @DisplayName("annotation has correct default message")
    void hasCorrectDefaultMessage() {
        ValidSubDept annotation = createAnnotation();
        assertThat(annotation.message()).isEqualTo("parentDeptPoid is mandatory when subdeptYN is 'Y'");
    }

    @Test
    @DisplayName("annotation has empty groups by default")
    void hasEmptyGroupsByDefault() {
        ValidSubDept annotation = createAnnotation();
        assertThat(annotation.groups()).isEmpty();
    }

    @Test
    @DisplayName("annotation has empty payload by default")
    void hasEmptyPayloadByDefault() {
        ValidSubDept annotation = createAnnotation();
        assertThat(annotation.payload()).isEmpty();
    }

    @Test
    @DisplayName("annotation is validated by SubDeptValidator")
    void isValidatedBySubDeptValidator() {
        ValidSubDept annotation = createAnnotation();
        assertThat(annotation.annotationType()).isEqualTo(ValidSubDept.class);
    }

    private ValidSubDept createAnnotation() {
        return new ValidSubDept() {
            @Override
            public String message() {
                return "parentDeptPoid is mandatory when subdeptYN is 'Y'";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidSubDept.class;
            }
        };
    }
}
