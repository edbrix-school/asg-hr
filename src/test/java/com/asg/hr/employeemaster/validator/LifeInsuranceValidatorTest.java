package com.asg.hr.employeemaster.validator;

import com.asg.hr.employeemaster.dto.EmployeeMasterRequestDto;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LifeInsuranceValidatorTest {

    private final LifeInsuranceValidator validator = new LifeInsuranceValidator();

    @Test
    void isValid_returnsTrueWhenDtoNull() {
        assertThat(validator.isValid(null, mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void isValid_returnsTrueWhenLifeInsuranceNotY() {
        EmployeeMasterRequestDto dto = new EmployeeMasterRequestDto();
        dto.setLifeInsurance("N");
        assertThat(validator.isValid(dto, mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void isValid_returnsFalseAndAddsViolationsWhenNomineeMissing() {
        EmployeeMasterRequestDto dto = new EmployeeMasterRequestDto();
        dto.setLifeInsurance("Y");
        dto.setInsuranceNominee(" ");
        dto.setInsuranceNomineeRelation(null);

        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder b1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder b2 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nb1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nb2 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(b1, b2);
        when(b1.addPropertyNode("insuranceNominee")).thenReturn(nb1);
        when(nb1.addConstraintViolation()).thenReturn(ctx);
        when(b2.addPropertyNode("insuranceNomineeRelation")).thenReturn(nb2);
        when(nb2.addConstraintViolation()).thenReturn(ctx);

        boolean ok = validator.isValid(dto, ctx);

        assertThat(ok).isFalse();
        verify(ctx).disableDefaultConstraintViolation();
        verify(ctx, times(2)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void isValid_returnsFalseWhenOnlyNomineeMissing() {
        EmployeeMasterRequestDto dto = new EmployeeMasterRequestDto();
        dto.setLifeInsurance("Y");
        dto.setInsuranceNominee(" ");
        dto.setInsuranceNomineeRelation("REL");

        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder b1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nb1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(b1);
        when(b1.addPropertyNode("insuranceNominee")).thenReturn(nb1);
        when(nb1.addConstraintViolation()).thenReturn(ctx);

        assertThat(validator.isValid(dto, ctx)).isFalse();
        verify(ctx).disableDefaultConstraintViolation();
        verify(ctx).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void isValid_returnsFalseWhenOnlyRelationMissing() {
        EmployeeMasterRequestDto dto = new EmployeeMasterRequestDto();
        dto.setLifeInsurance("Y");
        dto.setInsuranceNominee("NOM");
        dto.setInsuranceNomineeRelation(" ");

        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder b1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nb1 = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(b1);
        when(b1.addPropertyNode("insuranceNomineeRelation")).thenReturn(nb1);
        when(nb1.addConstraintViolation()).thenReturn(ctx);

        assertThat(validator.isValid(dto, ctx)).isFalse();
        verify(ctx).disableDefaultConstraintViolation();
        verify(ctx).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void isValid_returnsTrueWhenNomineeAndRelationPresent() {
        EmployeeMasterRequestDto dto = new EmployeeMasterRequestDto();
        dto.setLifeInsurance("Y");
        dto.setInsuranceNominee("Nom");
        dto.setInsuranceNomineeRelation("Rel");
        assertThat(validator.isValid(dto, mock(ConstraintValidatorContext.class))).isTrue();
    }
}

