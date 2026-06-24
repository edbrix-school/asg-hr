package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionUpdateRequest {

    private String description;

    private String remarks;

    private List<HrLunchDeductionDtlRequest> details;
}
