package com.asg.hr.employeeinduction.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private LovGetListDto employeeDet;
    private String createdBy;
    private LocalDateTime createdDate;
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
        private LovGetListDto inductionCategoryDet;
        private Long assigneePoid;
        private LovGetListDto assigneeDet;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private String status;
        private LovGetListDto statusDet;
        private String remarks;
    }
}
