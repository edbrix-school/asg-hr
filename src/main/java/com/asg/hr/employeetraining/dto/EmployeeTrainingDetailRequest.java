package com.asg.hr.employeetraining.dto;

import jakarta.validation.constraints.NotNull;
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
public class EmployeeTrainingDetailRequest {

    private Integer detRowId;

    @Size(max = 20, message = "Action type must not exceed 20 characters")
    private String actionType;

    @NotNull(message = "Employee is required in detail row")
    private Long empPoid;

    @NotNull(message = "Training status is required in detail row")
    @Size(max = 20, message = "Training status must not exceed 20 characters")
    private String trainingStatus;

    private LocalDate completedOn;

    @Size(max = 200, message = "Other remarks must not exceed 200 characters")
    private String otherRemarks;
}
