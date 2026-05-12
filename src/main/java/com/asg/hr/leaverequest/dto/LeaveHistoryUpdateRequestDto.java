package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveHistoryUpdateRequestDto {

    private Long transactionPoid;
    private String hrTicketIssueType;
    private LocalDate hrTicketTillDate;
    private BigDecimal hrTicketIssuedCount;
}
