package com.asg.hr.employeeinduction.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeInductionResponseDto {

    private Long poid;
    private String docId;
    private Long employeePoid;
    private String employeeName;
    private String remarks;
    private List<EmployeeInductionDetailResponseDto> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeInductionDetailResponseDto {
        private Integer sn;
        private String inductionCategory;
        private Long assigneePoid;
        private String assigneeName;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private String status;
        private String remarks;
    }
}