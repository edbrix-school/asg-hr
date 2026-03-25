package com.asg.hr.departmentmaster.validator;

import com.asg.hr.departmentmaster.annotation.ValidSubDept;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SubDeptValidator implements ConstraintValidator<ValidSubDept, HrDepartmentMasterRequest> {

    @Override
    public boolean isValid(HrDepartmentMasterRequest request, ConstraintValidatorContext context) {
        if (request == null) return true; // skip null

        if ("Y".equalsIgnoreCase(request.getSubdeptYN()) && request.getParentDeptPoid() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("parentDeptPoid is required when subdeptYN is 'Y'")
                    .addPropertyNode("parentDeptPoid")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}