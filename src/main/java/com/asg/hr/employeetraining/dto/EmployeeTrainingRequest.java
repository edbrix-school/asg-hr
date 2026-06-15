package com.asg.hr.employeetraining.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTrainingRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 200, message = "Course name must not exceed 200 characters")
    private String courseName;

    @NotNull(message = "Period from is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to is required")
    private LocalDate periodTo;

    private Integer durationDays;

    @NotBlank(message = "Training type is required")
    @Size(max = 30, message = "Training type must not exceed 30 characters")
    private String trainingType;

    @NotBlank(message = "Institution is required")
    @Size(max = 100, message = "Institution must not exceed 100 characters")
    private String institution;

    @DecimalMin(value = "0.0", inclusive = true, message = "Training cost cannot be negative")
    private BigDecimal trainingCost;

    @NotBlank(message = "Training location is required")
    @Size(max = 50, message = "Training location must not exceed 50 characters")
    private String trainingLocation;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotBlank(message = "Employee is required")
    @Size(max = 20, message = "Employee poid must not exceed 20 characters")
    private String employeePoid;

    @Valid
    private List<EmployeeTrainingDetailRequest> details = new ArrayList<>();
}
