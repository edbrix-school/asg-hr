package com.asg.hr.employeemaster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeDashboardListRequestDto {

    private Long designationPoid;
    private Long locationPoid;
    private Long departmentPoid;
    private LocalDate joinDateFrom;
    private LocalDate joinDateTo;
    private String status;
    private String filter;
}
