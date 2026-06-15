package com.asg.hr.employeemaster.dto;

import com.asg.hr.employeemaster.enums.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeExperienceDtlRequestDto {

    private ActionType actionType;
    private Long employeePoid;
    private Long detRowId;

    @NotBlank(message = "Employer Is Required")
    @Size(max = 200, message = "Employer Must Be At Most 200 Characters")
    private String employer;

    @NotBlank(message = "Country Location Is Required")
    @Size(max = 100, message = "Country Location Must Be At Most 100 Characters")
    private String countryLocation;

    @NotNull(message = "From Date Is Required")
    private LocalDate fromDate;

    @NotNull(message = "To Date Is Required")
    private LocalDate toDate;

    @Size(max = 10, message = "Months Must Be At Most 10 Characters")
    private String months;

    @NotBlank(message = "Designation Is Required")
    @Size(max = 200, message = "Designation Must Be At Most 200 Characters")
    private String designation;
}

