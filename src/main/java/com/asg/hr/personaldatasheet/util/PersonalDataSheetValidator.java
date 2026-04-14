package com.asg.hr.personaldatasheet.util;

import com.asg.common.lib.exception.ValidationException;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PersonalDataSheetValidator {

    public void validateRequest(PersonalDataSheetRequestDto request) {
        validateNomineePercentages(request);
        validateMandatoryFields(request);
    }

    private void validateNomineePercentages(PersonalDataSheetRequestDto request) {
        if (request.getNominees() != null && !request.getNominees().isEmpty()) {
            Map<String, Double> nomineeTypePercentages = request.getNominees().stream()
                    .filter(nominee -> nominee.getNomineeType() != null && nominee.getPercentage() != null)
                    .collect(Collectors.groupingBy(
                            PersonalDataSheetRequestDto.NomineeDto::getNomineeType,
                            Collectors.summingDouble(n -> n.getPercentage() != null ? n.getPercentage() : 0.0)
                    ));

            for (Map.Entry<String, Double> entry : nomineeTypePercentages.entrySet()) {
                if (Math.abs(entry.getValue() - 100.0) > 0.01) {
                    throw new ValidationException("Nominee percentages for type " + entry.getKey() + " must total 100%. Current total: " + entry.getValue());
                }
            }
        }
    }

    private void validateMandatoryFields(PersonalDataSheetRequestDto request) {
        if (request.getEmployeePoid() == null) {
            throw new ValidationException("Employee is required");
        }
        if (request.getEmployeeNamePassport() == null || request.getEmployeeNamePassport().trim().isEmpty()) {
            throw new ValidationException("Name as in Passport is required");
        }
        if (request.getResidentStatus() == null || request.getResidentStatus().trim().isEmpty()) {
            throw new ValidationException("Resident status is required");
        }
        if (request.getCurrentFlat() == null || request.getCurrentFlat().trim().isEmpty()) {
            throw new ValidationException("Flat/Village Number is required");
        }
        if (request.getCurrentBldg() == null || request.getCurrentBldg().trim().isEmpty()) {
            throw new ValidationException("Building is required");
        }
        if (request.getCurrentRoad() == null || request.getCurrentRoad().trim().isEmpty()) {
            throw new ValidationException("Road is required");
        }
        if (request.getCurrentBlock() == null || request.getCurrentBlock().trim().isEmpty()) {
            throw new ValidationException("Block is required");
        }
        if (request.getCurrentArea() == null || request.getCurrentArea().trim().isEmpty()) {
            throw new ValidationException("Area is required");
        }
        if (request.getCurrentMobile() == null || request.getCurrentMobile().trim().isEmpty()) {
            throw new ValidationException("Mobile Number is required");
        }
        if (request.getPermanentAddress() == null || request.getPermanentAddress().trim().isEmpty()) {
            throw new ValidationException("Permanent Address is required");
        }
        
        // Validate conditional mandatory nominee fields
        if (request.getNominees() != null && !request.getNominees().isEmpty()) {
            for (var nominee : request.getNominees()) {
                if (nominee.getNomineeName() == null || nominee.getNomineeName().trim().isEmpty()) {
                    throw new ValidationException("Primary Nominee is required");
                }
                if (nominee.getPercentage() == null) {
                    throw new ValidationException("Percentage is required");
                }
            }
        }
    }
}