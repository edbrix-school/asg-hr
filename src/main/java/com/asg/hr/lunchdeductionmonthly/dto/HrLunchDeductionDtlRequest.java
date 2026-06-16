package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionDtlRequest {

    private Long detRowId;

    private String actionType;

    private Long employeePoid;

    private String deductionType;

    private Long leaveDays;

    private BigDecimal amount;

    private String remarks;
}
