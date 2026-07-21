package com.asg.hr.attendancemonthly.dto;

import lombok.*;
import java.math.BigDecimal;
import com.asg.common.lib.dto.LovGetListDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendanceMonthlyDtlResponse {
    private Long detRowId;
    private Long transactionPoid;
    private Long employeePoid;
    private String employeeName;
    private Long totalDays;
    private Long absentDays;
    private BigDecimal overtime;
    private BigDecimal shortHours;
    private Long medicalDays;
    private Long leaveDays;
    private BigDecimal overtimeOt2;
    private Long weeklyOff;
    private Long governmentOff;
    private Long monthlyWokingDays;
    private Long deductDays;
    private BigDecimal excessHrs;
    private String remarks;
    private String drilldownLinkInfo;
    private BigDecimal netHours;
    private Long lateCounts;
    private BigDecimal lateDeduction;
    private BigDecimal shortHoursDed;
    private LovGetListDto empDtl;
}
