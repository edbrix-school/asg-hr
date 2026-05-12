package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveUpdateRequestDto {

    private Long transactionPoid;
    private String leaveDaysMethod;
    private Boolean annualEncashmentRight;

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

    private List<LeaveRequestDetailDto> details;

    private Boolean updateHistory;
    private Boolean cancelHistory;
}
