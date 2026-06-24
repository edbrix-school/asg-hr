package com.asg.hr.lunchdeductionmonthly.dto;

import com.asg.common.lib.dto.LovGetListDto;
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

    private LovGetListDto empDet;

    private String deductionType;

    private LovGetListDto deductionTypeDet;

    private Long lunchDays;

    private Long monthDays;

    private Long offDays;

    private Long totalDays;

    private BigDecimal costPerDay;

    private BigDecimal amount;

    private String remarks;
}
