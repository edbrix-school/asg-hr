package com.asg.hr.employeetraining.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTrainingDetailResponse {

    private Integer detRowId;
    private Long empPoid;
    private LovGetListDto empDet;
    private String trainingStatus;
    private LovGetListDto trainingStatusDet;
    private LocalDate completedOn;
    private String otherRemarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
