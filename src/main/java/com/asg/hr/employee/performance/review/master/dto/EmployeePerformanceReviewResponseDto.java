package com.asg.hr.employee.performance.review.master.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePerformanceReviewResponseDto {

    private Long competencyPoid;
    private Long groupPoid;
    private String competencyCode;
    private String competencyDescription;
    private String competencyNarration;
    private String active;
    private Integer seqNo;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
