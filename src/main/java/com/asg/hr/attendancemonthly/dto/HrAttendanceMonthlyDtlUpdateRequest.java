package com.asg.hr.attendancemonthly.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAttendanceMonthlyDtlUpdateRequest {
    private Long employeePoid;
    private Integer totalDays;
    private Integer absentDays;
    private BigDecimal overtime;
    private BigDecimal shortHours;
    private Integer medicalDays;
    private Integer leaveDays;
    private BigDecimal overtimeOt2;
    private Integer weeklyOff;
    private Integer governmentOff;
    private Integer monthlyWorkingDays;
    private Integer deductDays;
    private BigDecimal excessHrs;
    private String remarks;
    private BigDecimal netHours;
    private Integer lateCounts;
    private Integer lateDeduction;
    private BigDecimal shortHoursDed;

    private Long detRowId;
    private String actionType; // ISCREATED, ISUPDATED, ISDELETED
}
