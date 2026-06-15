package com.asg.hr.lunchdeductionmonthly.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrLunchDeductionLoadDto {

    private List<HrLunchDeductionDtlResponse> lunchDetails;
}
