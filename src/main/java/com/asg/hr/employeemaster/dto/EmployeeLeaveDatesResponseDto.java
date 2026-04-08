package com.asg.hr.employeemaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeaveDatesResponseDto {
    private String startDate;
    private String periodEndDate;
}
