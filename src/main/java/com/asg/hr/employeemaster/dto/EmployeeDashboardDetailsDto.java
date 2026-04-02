package com.asg.hr.employeemaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class EmployeeDashboardDetailsDto {

    private Long employeePoid;
    private String employeeName;
    private String employeeName2;
    private Long designationPoid;
    private Long locationPoid;
    private Long departmentPoid;
    private LocalDate joinDate;
    private String mobile;
    private byte[] photo;
    private String active;
}