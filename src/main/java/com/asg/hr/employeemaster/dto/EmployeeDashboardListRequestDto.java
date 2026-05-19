package com.asg.hr.employeemaster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeDashboardListRequestDto {

    private String designationName;
    private String locationName;
    private String deptName;
    private LocalDate joinDateFrom;
    private LocalDate joinDateTo;
    private String status;
    private String filter;
}
