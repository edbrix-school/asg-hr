package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDependentsDtlResponseDto {

    private Long employeePoid;
    private Long detRowId;

    private String name;
    private LocalDate dateOfBirth;
    private String relation;
    private String gender;
    private String nationality;
    private String passportNo;
    private LocalDate ppExpiryDate;

    private String cprNo;
    private LocalDate cprExpiry;

    private String insuDetails;
    private LocalDate insuStartDt;
    private String sponsor;
    private LocalDate rpExpiry;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

