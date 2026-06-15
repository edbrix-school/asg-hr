package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionUpdateRequest {

    private String description;

    private String remarks;
}
