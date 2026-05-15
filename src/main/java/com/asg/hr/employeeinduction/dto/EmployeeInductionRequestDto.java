package com.asg.hr.employeeinduction.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeInductionRequestDto {

    private Long poid;
    private String docId;
    private Long employeePoid;
    private String remarks;
    private List<EmployeeInductionDetailRequestDto> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeInductionDetailRequestDto {
        private Integer sn;
        private String inductionCategory;
        private Long assigneePoid;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private String status;
        private String remarks;
        private String actionType;
    }
}
