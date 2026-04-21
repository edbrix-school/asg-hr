package com.asg.hr.resignation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrResignationRequest {

    @NotNull(message = "Employee Poid is mandatory")
    @Positive(message = "Employee Poid must be positive")
    private Long employeePoid;

    @NotNull(message = "Last date of work is mandatory")
    private LocalDate lastDateOfWork;

    @NotNull(message = "Transaction date is mandatory")
    private LocalDate transactionDate;

    @NotBlank(message = "Resignation details is mandatory")
    @Size(max = 1000, message = "Resignation details must not exceed 1000 characters")
    private String resignationDetails;

    @NotBlank(message = "Resignation type is mandatory")
    @Size(max = 100, message = "Resignation type must not exceed 100 characters")
    private String resignationType;

    @Size(max = 1000, message = "HOD remarks must not exceed 1000 characters")
    private String hodRemarks;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}

