package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveTicketUpdateRequestDto {

    private Long transactionPoid;
    private String ticketBookBy;
    private String ticketProcessed;
    private String ticketRemarks;
    private BigDecimal ticketsIssued;
    private String pjDocRef;
}
