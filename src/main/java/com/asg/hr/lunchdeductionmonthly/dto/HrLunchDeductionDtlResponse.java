package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionDtlResponse {

    private Long detRowId;

    private Long transactionPoid;

    private Long employeePoid;

    private String employeeName;

    private String deductionType;

    private Long lunchDays;

    private Long monthDays;

    private Long offDays;

    private Long totalDays;

    private BigDecimal costPerDay;

    private BigDecimal amount;      // maps to LUNCH_DEDCTION_AMT in DB

    private String remarks;
}
