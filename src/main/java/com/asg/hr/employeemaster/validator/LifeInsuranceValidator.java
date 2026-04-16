package com.asg.hr.employeemaster.validator;

import com.asg.hr.employeemaster.annotation.ValidLifeInsurance;
import com.asg.hr.employeemaster.dto.EmployeeMasterRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class LifeInsuranceValidator implements ConstraintValidator<ValidLifeInsurance, EmployeeMasterRequestDto> {

    @Override
    public boolean isValid(EmployeeMasterRequestDto dto, ConstraintValidatorContext context) {

        if (dto == null) {
            return true;
        }

        if ("Y".equalsIgnoreCase(dto.getLifeInsurance()) && (StringUtils.isBlank(dto.getInsuranceNominee()) ||
                StringUtils.isBlank(dto.getInsuranceNomineeRelation()))) {

            context.disableDefaultConstraintViolation();

            if (StringUtils.isBlank(dto.getInsuranceNominee())) {
                context.buildConstraintViolationWithTemplate("Insurance Nominee is required when Life Insurance is Y")
                        .addPropertyNode("insuranceNominee")
                        .addConstraintViolation();
            }

            if (StringUtils.isBlank(dto.getInsuranceNomineeRelation())) {
                context.buildConstraintViolationWithTemplate("Insurance Nominee Relation is required when Life Insurance is Y")
                        .addPropertyNode("insuranceNomineeRelation")
                        .addConstraintViolation();
            }
            return false;
        }

        return true;
    }
}