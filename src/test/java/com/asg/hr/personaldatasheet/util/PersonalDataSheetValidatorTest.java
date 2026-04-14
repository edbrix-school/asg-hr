package com.asg.hr.personaldatasheet.util;

import com.asg.common.lib.exception.ValidationException;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PersonalDataSheetValidatorTest {

    @InjectMocks
    private PersonalDataSheetValidator validator;

    private PersonalDataSheetRequestDto validRequest;

    @BeforeEach
    void setUp() {
        validRequest = createValidRequest();
    }

    @Test
    void validateRequest_WithValidData_ShouldNotThrowException() {
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    // Mandatory Fields Tests
    @Test
    void validateRequest_WithNullEmployeePoid_ShouldThrowValidationException() {
        validRequest.setEmployeePoid(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Employee is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullEmployeeNamePassport_ShouldThrowValidationException() {
        validRequest.setEmployeeNamePassport(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Name as in Passport is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyEmployeeNamePassport_ShouldThrowValidationException() {
        validRequest.setEmployeeNamePassport("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Name as in Passport is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithWhitespaceEmployeeNamePassport_ShouldThrowValidationException() {
        validRequest.setEmployeeNamePassport("   ");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Name as in Passport is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullResidentStatus_ShouldThrowValidationException() {
        validRequest.setResidentStatus(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Resident status is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyResidentStatus_ShouldThrowValidationException() {
        validRequest.setResidentStatus("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Resident status is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullCurrentFlat_ShouldThrowValidationException() {
        validRequest.setCurrentFlat(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Flat/Village Number is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyCurrentFlat_ShouldThrowValidationException() {
        validRequest.setCurrentFlat("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Flat/Village Number is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullCurrentBldg_ShouldThrowValidationException() {
        validRequest.setCurrentBldg(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Building is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyCurrentBldg_ShouldThrowValidationException() {
        validRequest.setCurrentBldg("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Building is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullCurrentRoad_ShouldThrowValidationException() {
        validRequest.setCurrentRoad(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Road is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyCurrentRoad_ShouldThrowValidationException() {
        validRequest.setCurrentRoad("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Road is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullCurrentBlock_ShouldThrowValidationException() {
        validRequest.setCurrentBlock(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Block is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyCurrentBlock_ShouldThrowValidationException() {
        validRequest.setCurrentBlock("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Block is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullCurrentArea_ShouldThrowValidationException() {
        validRequest.setCurrentArea(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Area is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyCurrentArea_ShouldThrowValidationException() {
        validRequest.setCurrentArea("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Area is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullCurrentMobile_ShouldThrowValidationException() {
        validRequest.setCurrentMobile(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Mobile Number is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyCurrentMobile_ShouldThrowValidationException() {
        validRequest.setCurrentMobile("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Mobile Number is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNullPermanentAddress_ShouldThrowValidationException() {
        validRequest.setPermanentAddress(null);
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Permanent Address is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithEmptyPermanentAddress_ShouldThrowValidationException() {
        validRequest.setPermanentAddress("");
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Permanent Address is required", exception.getMessage());
    }

    // Nominee Validation Tests
    @Test
    void validateRequest_WithNullNominees_ShouldNotThrowException() {
        validRequest.setNominees(null);
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    @Test
    void validateRequest_WithEmptyNominees_ShouldNotThrowException() {
        validRequest.setNominees(Collections.emptyList());
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    @Test
    void validateRequest_WithNomineeNullName_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setNomineeName(null);
        nominee.setPercentage(100.0);
        nominee.setNomineeType("PRIMARY");
        validRequest.setNominees(Arrays.asList(nominee));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Primary Nominee is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNomineeEmptyName_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setNomineeName("");
        nominee.setPercentage(100.0);
        nominee.setNomineeType("PRIMARY");
        validRequest.setNominees(Arrays.asList(nominee));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Primary Nominee is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNomineeWhitespaceName_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setNomineeName("   ");
        nominee.setPercentage(100.0);
        nominee.setNomineeType("PRIMARY");
        validRequest.setNominees(Arrays.asList(nominee));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Primary Nominee is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNomineeNullPercentage_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setNomineeName("John Doe");
        nominee.setPercentage(null);
        nominee.setNomineeType("PRIMARY");
        validRequest.setNominees(Arrays.asList(nominee));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Percentage is required", exception.getMessage());
    }

    // Nominee Percentage Validation Tests
    @Test
    void validateRequest_WithValidNomineePercentages_ShouldNotThrowException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 60.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 40.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    @Test
    void validateRequest_WithNomineePercentagesNotTotaling100_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 60.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 30.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Nominee percentages for type PRIMARY must total 100%. Current total: 90.0", exception.getMessage());
    }

    @Test
    void validateRequest_WithNomineePercentagesExceeding100_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 60.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 50.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Nominee percentages for type PRIMARY must total 100%. Current total: 110.0", exception.getMessage());
    }

    @Test
    void validateRequest_WithMultipleNomineeTypesValidPercentages_ShouldNotThrowException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 100.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "SECONDARY", 100.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    @Test
    void validateRequest_WithMultipleNomineeTypesInvalidPercentages_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 80.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "SECONDARY", 90.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertTrue(exception.getMessage().contains("must total 100%"));
    }

    @Test
    void validateRequest_WithNomineePercentageWithinTolerance_ShouldNotThrowException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 99.99);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 0.01);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    @Test
    void validateRequest_WithNomineePercentageOutsideTolerance_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", 99.0);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 0.5);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Nominee percentages for type PRIMARY must total 100%. Current total: 99.5", exception.getMessage());
    }

    @Test
    void validateRequest_WithNomineeNullType_ShouldNotThrowException() {
        PersonalDataSheetRequestDto.NomineeDto nominee = new PersonalDataSheetRequestDto.NomineeDto();
        nominee.setNomineeName("John Doe");
        nominee.setPercentage(100.0);
        nominee.setNomineeType(null);
        validRequest.setNominees(Arrays.asList(nominee));
        
        assertDoesNotThrow(() -> validator.validateRequest(validRequest));
    }

    @Test
    void validateRequest_WithNomineeNullPercentageInList_ShouldThrowValidationException() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = createNominee("John Doe", "PRIMARY", null);
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 100.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        // This should fail because nominee1 has null percentage, which is mandatory
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Percentage is required", exception.getMessage());
    }

    @Test
    void validateRequest_WithNomineeNullTypeAndPercentage_ShouldNotAffectPercentageValidation() {
        PersonalDataSheetRequestDto.NomineeDto nominee1 = new PersonalDataSheetRequestDto.NomineeDto();
        nominee1.setNomineeName("John Doe");
        nominee1.setNomineeType(null); // null type
        nominee1.setPercentage(null); // null percentage
        
        PersonalDataSheetRequestDto.NomineeDto nominee2 = createNominee("Jane Doe", "PRIMARY", 100.0);
        validRequest.setNominees(Arrays.asList(nominee1, nominee2));
        
        // This should fail because nominee1 has null percentage, which is mandatory
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateRequest(validRequest));
        assertEquals("Percentage is required", exception.getMessage());
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