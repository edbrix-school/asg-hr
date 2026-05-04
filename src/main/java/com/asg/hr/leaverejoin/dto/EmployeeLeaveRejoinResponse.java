package com.asg.hr.leaverejoin.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeaveRejoinResponse {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private Long companyPoid;

    private Long employeePoid;
    private LovGetListDto employeeDet;
    private String designationName;
    private String departmentName;

    private Long leaveRequestPoid;
    private LovGetListDto leaveRequestDet;
    private LocalDate dateProceededOnLeave;
    private LocalDate plannedRejoinDate;

    private LocalDate dateOfRejoining;
    private String remarks;

    private String passportReceived;
    private LovGetListDto passportReceivedDet;
    private String receivedBy;
    private String remarksByHr;

    private Integer extraLeaveDays;
    private Integer extraAbsentDays;

    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
}
