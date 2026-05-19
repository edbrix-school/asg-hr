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
    private String designationName;
    private String locationName;
    private String deptName;
    private LocalDate joinDate;
    private String mobile;
    private byte[] photo;
    private String active;
}