package com.asg.hr.resignation.dto;

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
public class HrResignationResponse {

    private Long transactionPoid;
    private String docRef;
    private LocalDate transactionDate;

    private Long employeePoid;
    private LovGetListDto employeeDet;
    private Long departmentPoid;
    private LovGetListDto departmentDet;
    private Long designationPoid;
    private LovGetListDto designationDet;
    private Long directSupervisorPoid;
    private LovGetListDto directSupervisorDet;

    private LocalDate lastDateOfWork;
    private String resignationDetails;

    private LocalDate joinDate;
    private LocalDate rpExpiryDate;

    private String resignationType;
    private LovGetListDto resignationTypeDet;
    private String hodRemarks;
    private String remarks;

    private String deleted;

    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;

}

