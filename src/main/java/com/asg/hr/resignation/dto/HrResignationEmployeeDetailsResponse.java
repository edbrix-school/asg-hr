package com.asg.hr.resignation.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrResignationEmployeeDetailsResponse {

    private Long departmentPoid;
    private LovGetListDto departmentDet;
    private Long designationPoid;
    private LovGetListDto designationDet;
    private Long directSupervisorPoid;
    private LovGetListDto directSupervisorDet;
    private LocalDate joinDate;
    private LocalDate rpExpiryDate;
    // Legacy default value assigned after employee selection
    private String resignationType;
    private LovGetListDto resignationTypeDet;
    private String status;
}

