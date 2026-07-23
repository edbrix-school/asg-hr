package com.asg.hr.lunchdeductionmonthly.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionResponse {

    private Long transactionPoid;

    private String docRef;

    private LocalDate transactionDate;

    private LocalDate payrollMonth;

    private String description;

    private String remarks;

    private String createdBy;

    private LocalDateTime createdDate;

    private List<HrLunchDeductionDtlResponse> details;
}
