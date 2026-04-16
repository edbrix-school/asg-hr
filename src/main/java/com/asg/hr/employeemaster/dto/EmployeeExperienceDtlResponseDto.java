package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeExperienceDtlResponseDto {

    private Long employeePoid;
    private Long detRowId;

    private String employer;
    private String countryLocation;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String months;
    private String designation;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

