package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveCalculationResponseDto {

    private Boolean leaveHolidayRunning;
    private LocalDate leaveStartDate;
    private LocalDate planedRejoinDate;
    private BigDecimal calendarDays;
    private BigDecimal holidays;
    private BigDecimal leaveDays;
    private BigDecimal balanceTillRejoin;
}
