package com.asg.hr.leaverejoin.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeaveRejoinEmployeeDetailsResponse {

    private Long employeePoid;
    private LovGetListDto employeeDet;
    private String designationName;
    private String departmentName;
    private String status;
}
