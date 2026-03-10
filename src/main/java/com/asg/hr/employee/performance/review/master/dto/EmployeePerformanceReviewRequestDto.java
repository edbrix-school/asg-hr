package com.asg.hr.employee.performance.review.master.dto;

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

    private String active;

    private Integer seqNo;
}
