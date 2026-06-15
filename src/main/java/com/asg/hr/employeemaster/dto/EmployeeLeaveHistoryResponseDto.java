package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeaveHistoryResponseDto {

    private Long leaveHistPoid;
    private Long detRowId;

    private Long companyPoid;
    private Long groupPoid;
    private Long deptPoid;
    private Long employeePoid;

    private String employeeName;
    private LocalDate leaveStartDate;
    private LocalDate rejoinDate;
    private String reffNo;
    private String remarks;
    private String deleted;
    private String leaveType;
    private Integer leaveDays;
    private String annualLeaveType;

    private String sourceDocId;
    private Long sourceDocPoid;

    private String emergencyLeaveType;
    private LocalDate expRejoinDate;
    private String splLeaveTypes;

    private Integer eligibleLeaveDays;
    private Integer ticketIssuedCount;
    private LocalDate ticketTillDate;
    private String ticketIssueType;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

