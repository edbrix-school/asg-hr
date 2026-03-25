package com.asg.hr.employeemaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "HR_EMPLOYEE_LEAVE_HISTORY")
@IdClass(HrEmployeeLeaveHistoryId.class)
public class HrEmployeeLeaveHistory extends BaseEntity {

    @Id
    @Column(name = "LEAVE_HIST_POID", nullable = false)
    private Long leaveHistPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "DEPT_POID")
    private Long deptPoid;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "EMPLOYEE_NAME", length = 100)
    private String employeeName;

    @Column(name = "LEAVE_START_DATE")
    private LocalDate leaveStartDate;

    @Column(name = "REJOIN_DATE")
    private LocalDate rejoinDate;

    @Column(name = "REFF_NO", length = 20)
    private String reffNo;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "LEAVE_TYPE", length = 20)
    private String leaveType;

    @Column(name = "LEAVE_DAYS")
    private Integer leaveDays;

    @Column(name = "ANNUAL_LEAVE_TYPE", length = 20)
    private String annualLeaveType;

    @Column(name = "SOURCE_DOC_ID", length = 20)
    private String sourceDocId;

    @Column(name = "SOURCE_DOC_POID")
    private Long sourceDocPoid;

    @Column(name = "EMERGENCY_LEAVE_TYPE", length = 50)
    private String emergencyLeaveType;

    @Column(name = "EXP_REJOIN_DATE")
    private LocalDate expRejoinDate;

    @Column(name = "SPL_LEAVE_TYPES", length = 30)
    private String splLeaveTypes;

    @Column(name = "ELIGIBLE_LEAVE_DAYS")
    private Integer eligibleLeaveDays;

    @Column(name = "TICKET_ISSUED_COUNT")
    private Integer ticketIssuedCount;

    @Column(name = "TICKET_TILL_DATE")
    private LocalDate ticketTillDate;

    @Column(name = "TICKET_ISSUE_TYPE", length = 100)
    private String ticketIssueType;
}