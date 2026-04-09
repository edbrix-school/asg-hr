package com.asg.hr.lunchdeduction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchDeductionDetailResponseDto {

    private Long detRowId;
    private Long employeePoid;
    private String deductionType;
    private Long monthDays;
    private Long offDays;
    private Long totalDays;
    private BigDecimal costPerDay;
    private BigDecimal lunchDeductionAmt;
    private String remarks;
    private Long lunchDays;
    private String userId;
    private String userName;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}