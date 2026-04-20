package com.asg.hr.leaverequest.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;



@Entity
@Table(name = "HR_LEAVE_REQUEST_HDR")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class HrLeaveRequestHdrEntity extends BaseEntity {


    @Id
    @Column(name = "TRANSACTION_POID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "LEAVE_TYPE")
    private String leaveType;

    @Column(name = "CONTACT_NUMBER")
    private String contactNumber;

    @Column(name = "LEAVE_START_DATE")
    private LocalDate leaveStartDate;

    @Column(name = "PLANED_REJOIN_DATE")
    private LocalDate planedRejoinDate;

    @Column(name = "LEAVE_DAYS")
    private BigDecimal leaveDays;

    @Column(name = "ELIGIBLE_LEAVE_DAYS")
    private BigDecimal eligibleLeaveDays;

    @Column(name = "BALANCE_LEAVE_DAYS")
    private BigDecimal balanceLeaveDays;

    @Column(name = "TICKET_ELIGIBLITY")
    private BigDecimal ticketEligiblity;

    @Column(name = "AIR_SECTOR_POID")
    private Long airSectorPoid;

    @Column(name = "TICKET_BOOKBY")
    private String ticketBookBy;

    @Column(name = "BOOKED_TICKET")
    private String bookedTicket;

    @Column(name = "HOD_REMARKS")
    private String hodRemarks;

    @Column(name = "MEDICAL_RECORDS_ATTACHED")
    private String medicalRecordsAttached;

    @Column(name = "OTHER_LEAVE_REASON")
    private String otherLeaveReason;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DOC_REF")
    private String docRef;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "ANNUAL_LEAVE_TYPE")
    private String annualLeaveType;

    @Column(name = "EMERGENCY_LEAVE_TYPE")
    private String emergencyLeaveType;

    @Column(name = "SPL_LEAVE_TYPES")
    private String splLeaveTypes;

    @Column(name = "UPDATE_REJOIN_DATE")
    private String updateRejoinDate;

    @Column(name = "TICKET_REQUIRED")
    private String ticketRequired;

    @Column(name = "TICKET_FROM_LOCN")
    private String ticketFromLocn;

    @Column(name = "TICKET_TO_LOCN")
    private String ticketToLocn;

    @Column(name = "TICKET_TRAVEL_DATE")
    private LocalDate ticketTravelDate;

    @Column(name = "TICKET_RETURN_DATE")
    private LocalDate ticketReturnDate;

    @Column(name = "TICKET_COUNT")
    private BigDecimal ticketCount;

    @Column(name = "TICKET_REMARKS")
    private String ticketRemarks;

    @Column(name = "TICKET_EARNED")
    private BigDecimal ticketEarned;

    @Column(name = "TICKET_PERIOD")
    private BigDecimal ticketPeriod;

    @Column(name = "SETTLEMENT_POID")
    private Long settlementPoid;

    @Column(name = "TICKET_PROCESSED")
    private String ticketProcessed;

    @Column(name = "PAID_LEAVES")
    private BigDecimal paidLeaves;

    @Column(name = "MEDICAL_ELIGIBLE")
    private BigDecimal medicalEligible;

    @Column(name = "MEDICAL_TAKEN")
    private BigDecimal medicalTaken;

    @Column(name = "MEDICAL_BALANCE")
    private BigDecimal medicalBalance;

    @Column(name = "LAST_LEAVE_DETAILS")
    private String lastLeaveDetails;

    @Column(name = "LAST_TICKET_DETAILS")
    private String lastTicketDetails;

    @Column(name = "TICKETS_ISSUED")
    private BigDecimal ticketsIssued;

    @Column(name = "PJ_DOC_REF")
    private String pjDocRef;

    @Column(name = "HR_TICKET_ISSUE_TYPE")
    private String hrTicketIssueType;

    @Column(name = "HR_TICKET_ISSUED_COUNT")
    private BigDecimal hrTicketIssuedCount;

    @Column(name = "HR_TICKET_ENCASHMENT")
    private BigDecimal hrTicketEncashment;

    @Column(name = "HR_TICKETS_EARNED")
    private BigDecimal hrTicketsEarned;

    @Column(name = "HR_TICKET_TILL_DATE")
    private LocalDate hrTicketTillDate;

    @Column(name = "HOD")
    private Long hod;

    @Column(name = "CALENDAR_DAYS")
    private BigDecimal calendarDays;

    @Column(name = "HOLIDAYS")
    private BigDecimal holidays;

    @Column(name = "BALANCE_TILL_REJOIN")
    private BigDecimal balanceTillRejoin;
}
