package com.asg.hr.Employee.performance.review.master.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePerformanceReviewRequestDto {

    @NotBlank(message = "Competency code is required")
    private String competencyCode;

    private String competencyDescription;

    private String competencyNarration;

    private Integer seqNo;
}
