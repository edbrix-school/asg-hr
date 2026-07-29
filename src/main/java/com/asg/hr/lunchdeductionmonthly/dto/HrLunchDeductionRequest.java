package com.asg.hr.lunchdeductionmonthly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionRequest {

    @NotNull(message = "Payroll month is mandatory")
    private LocalDate payrollMonth;

    private LocalDate transactionDate;

    private String description;

    private String remarks;

    private List<HrLunchDeductionDtlRequest> details;
}
