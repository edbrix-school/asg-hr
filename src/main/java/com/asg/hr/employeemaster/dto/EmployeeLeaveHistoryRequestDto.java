package com.asg.hr.employeemaster.dto;

import com.asg.hr.employeemaster.enums.ActionType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLeaveHistoryRequestDto {

    private ActionType actionType;

    // Required for isUpdated/isDeleted. If null for isCreated, backend will generate.
    private Long leaveHistPoid;
    private Long detRowId;

    private Long companyPoid;
    private Long groupPoid;
    private Long deptPoid;
    private Long employeePoid;

    @Size(max = 100, message = "Employee Name Must Be At Most 100 Characters")
    private String employeeName;

    private LocalDate leaveStartDate;
    private LocalDate rejoinDate;

    @Size(max = 20, message = "Reff No Must Be At Most 20 Characters")
    private String reffNo;

    @Size(max = 100, message = "Remarks Must Be At Most 100 Characters")
    private String remarks;

    @Size(max = 20, message = "Leave Type Must Be At Most 20 Characters")
    private String leaveType;
    private Integer leaveDays;

    @Size(max = 20, message = "Annual Leave Type Must Be At Most 20 Characters")
    private String annualLeaveType;

    @Size(max = 20, message = "Source Doc Id Must Be At Most 20 Characters")
    private String sourceDocId;
    private Long sourceDocPoid;

    @Size(max = 50, message = "Emergency Leave Type Must Be At Most 50 Characters")
    private String emergencyLeaveType;
    private LocalDate expRejoinDate;

    @Size(max = 30, message = "Spl Leave Types Must Be At Most 30 Characters")
    private String splLeaveTypes;

    @Size(max = 1, message = "Deleted Must Be At Most 1 Character")
    private String deleted;

    private Integer eligibleLeaveDays;
    private Integer ticketIssuedCount;
    private LocalDate ticketTillDate;

    @Size(max = 100, message = "Ticket Issue Type Must Be At Most 100 Characters")
    private String ticketIssueType;
}

