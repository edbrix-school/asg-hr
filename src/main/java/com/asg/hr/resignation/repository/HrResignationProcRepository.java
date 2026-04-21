package com.asg.hr.resignation.repository;

import com.asg.hr.resignation.dto.HrResignationEmployeeDetailsResponse;

import java.time.LocalDate;

public interface HrResignationProcRepository {

    HrResignationEmployeeDetailsResponse getEmployeeDetails(Long employeePoid);

    String beforeSaveValidation(
            Long companyPoid,
            Long userPoid,
            LocalDate docDate,
            Long transactionPoid,
            Long employeePoid,
            LocalDate lastDateOfWork
    );
}
