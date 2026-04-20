package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveResponseDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private Long employeePoid;

    private String leaveType;
    private String annualLeaveType;
    private String emergencyLeaveType;
    private String splLeaveTypes;

    private LocalDate leaveStartDate;
    private LocalDate planedRejoinDate;
    private String updateRejoinDate;

    private BigDecimal leaveDays;
    private BigDecimal eligibleLeaveDays;
    private BigDecimal balanceTillRejoin;
    private BigDecimal balanceLeaveDays;
    private BigDecimal calendarDays;
    private BigDecimal holidays;

    private String contactNumber;
    private String medicalRecordsAttached;
    private String otherLeaveReason;
    private String hodRemarks;
    private Long hod;

    private String ticketRequired;
    private String ticketFromLocn;
    private String ticketToLocn;
    private LocalDate ticketTravelDate;
    private LocalDate ticketReturnDate;

    private BigDecimal ticketCount;
    private String ticketRemarks;
    private BigDecimal ticketEligiblity;
    private BigDecimal ticketEarned;
    private BigDecimal ticketPeriod;
    private Long settlementPoid;
    private String bookedTicket;
    private Long airSectorPoid;
    private String ticketBookBy;
    private String ticketProcessed;
    private BigDecimal ticketsIssued;
    private String pjDocRef;
    private BigDecimal paidLeaves;
    private BigDecimal medicalEligible;
    private BigDecimal medicalTaken;
    private BigDecimal medicalBalance;
    private String lastLeaveDetails;
    private String lastTicketDetails;
    private String hrTicketIssueType;
    private BigDecimal hrTicketIssuedCount;
    private BigDecimal hrTicketEncashment;
    private BigDecimal hrTicketsEarned;
    private LocalDate hrTicketTillDate;

    private String status;

    private List<LeaveRequestDetailDto> details;
}
