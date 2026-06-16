package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionResponse {

    private Long transactionPoid;

    private String docRef;

    private LocalDate payrollMonth;

    private String description;

    private String remarks;

    private List<HrLunchDeductionDtlResponse> details;
}
