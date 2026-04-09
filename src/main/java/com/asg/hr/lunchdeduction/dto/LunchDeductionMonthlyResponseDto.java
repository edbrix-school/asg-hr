package com.asg.hr.lunchdeduction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchDeductionMonthlyResponseDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private LocalDate transactionDate;
    private String docRef;
    private LocalDate payrollMonth;
    private String lunchDescription;
    private String remarks;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    @Builder.Default
    private List<LunchDeductionDetailResponseDto> details = new ArrayList<>();
    private Long detailCount;
    private Long totalLunchDays;
    private BigDecimal totalDeductionAmount;
}