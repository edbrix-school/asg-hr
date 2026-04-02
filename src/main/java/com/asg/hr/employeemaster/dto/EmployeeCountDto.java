package com.asg.hr.employeemaster.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class EmployeeCountDto {

    private Long totalEmployees;
    private Long activeEmployees;
    private Long inactiveEmployees;

}