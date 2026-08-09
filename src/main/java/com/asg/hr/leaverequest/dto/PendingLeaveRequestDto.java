package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PendingLeaveRequestDto {

    private Long transactionPoid;
    private String docRef;
    private String leaveType;
    private LocalDate leaveStartDate;
    private LocalDate planedRejoinDate;
    private String status;
}
