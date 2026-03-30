package com.asg.hr.departmentmaster.validator;

import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubDeptValidatorTest {

    private SubDeptValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new SubDeptValidator();
    }

    @Test
    @DisplayName("returns true when request is null")
    void returnsTrue_whenRequestIsNull() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("returns true when subdeptYN is not Y")
    void returnsTrue_whenSubdeptYNIsNotY() {
        HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                .subdeptYN("N")
                .parentDeptPoid(null)
                .build();

        assertTrue(validator.isValid(request, context));
    }

    @Test
    @DisplayName("returns true when subdeptYN is Y and parentDeptPoid is provided")
    void returnsTrue_whenSubdeptYNIsYAndParentDeptPoidProvided() {
        HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                .subdeptYN("Y")
                .parentDeptPoid(100L)
                .build();

        assertTrue(validator.isValid(request, context));
    }

    @Test
    @DisplayName("returns false when subdeptYN is Y and parentDeptPoid is null")
    void returnsFalse_whenSubdeptYNIsYAndParentDeptPoidIsNull() {
        HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                .subdeptYN("Y")
                .parentDeptPoid(null)
                .build();

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(request, context));

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("parentDeptPoid is required when subdeptYN is 'Y'");
        verify(violationBuilder).addPropertyNode("parentDeptPoid");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("returns false when subdeptYN is y (lowercase) and parentDeptPoid is null")
    void returnsFalse_whenSubdeptYNIsLowercaseYAndParentDeptPoidIsNull() {
        HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                .subdeptYN("y")
                .parentDeptPoid(null)
                .build();

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(request, context));
    }

    @Test
    @DisplayName("returns true when subdeptYN is null")
    void returnsTrue_whenSubdeptYNIsNull() {
        HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                .subdeptYN(null)
                .parentDeptPoid(null)
                .build();

        assertTrue(validator.isValid(request, context));
    }

    @Test
    @DisplayName("returns true when subdeptYN is empty string")
    void returnsTrue_whenSubdeptYNIsEmpty() {
        HrDepartmentMasterRequest request = HrDepartmentMasterRequest.builder()
                .subdeptYN("")
                .parentDeptPoid(null)
                .build();

        assertTrue(validator.isValid(request, context));
    }
}
