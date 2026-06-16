package com.asg.hr.lunchdeductionmonthly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionRequest {

    @NotNull(message = "Payroll month is mandatory")
    private LocalDate payrollMonth;

    private String description;

    private String remarks;
}
