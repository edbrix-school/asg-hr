package com.asg.hr.personaldatasheet.service;

import com.asg.common.lib.exception.ValidationException;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.util.PersonalDataSheetValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ValidatorTestRunner {
    
    @Test
    void testPercentageValidation() {
        PersonalDataSheetValidator validator = new PersonalDataSheetValidator();
        PersonalDataSheetRequestDto request = createValidRequest();
        
        // Test case that should be outside tolerance
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 99.98);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 0.005);
        request.setNominees(Arrays.asList(nominee1, nominee2));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(request));

    }
    
    private PersonalDataSheetRequestDto createValidRequest() {
        PersonalDataSheetRequestDto request = new PersonalDataSheetRequestDto();
        request.setEmployeePoid(1L);
        request.setEmployeeNamePassport("John Doe");
        request.setResidentStatus("Resident");
        request.setCurrentFlat("123");
        request.setCurrentBldg("Building A");
        request.setCurrentRoad("Main Street");
        request.setCurrentBlock("Block 1");
        request.setCurrentArea("Downtown");
        request.setCurrentMobile("12345678");
        request.setPermanentAddress("123 Main Street");
        return request;
    }

    private PersonalDataSheetRequestDto.NomineeDto createNominee(String name, String type, Double percentage) {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setNomineeName(name);
        nominee.setNomineeType(type);
        nominee.setPercentage(percentage);
        return nominee;
    }
}