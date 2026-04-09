package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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