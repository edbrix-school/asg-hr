package com.asg.hr.leaverejoin.dto;

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
public class EmployeeLeaveRejoinLeaveDetailsResponse {

    private Long employeePoid;
    private LovGetListDto employeeDet;
    private Long leaveRequestPoid;
    private LovGetListDto leaveRequestDet;
    private LocalDate dateProceededOnLeave;
    private LocalDate plannedRejoinDate;
    private String status;
}
