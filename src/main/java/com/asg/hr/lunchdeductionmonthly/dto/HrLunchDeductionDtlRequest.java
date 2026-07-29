package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionDtlRequest {

    private Long detRowId;

    private String employeePoid;

    private String employeeName;

    private String deductionType;

    private Long lunchDays;

    private Long monthDays;

    private Long leaveDays;

    private Long totalDays;

    private BigDecimal costPerDay;

    private BigDecimal amount;

    private String remarks;

    private String actionType;
}
