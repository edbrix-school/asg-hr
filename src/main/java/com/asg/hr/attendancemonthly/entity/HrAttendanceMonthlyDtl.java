package com.asg.hr.attendancemonthly.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.hr.attendancemonthly.entity.key.HrAttendanceMonthlyDtlKey;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "HR_ATTENDANCE_MONTHLY_DTL")
@IdClass(HrAttendanceMonthlyDtlKey.class)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAttendanceMonthlyDtl extends BaseEntity {

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "TOTAL_DAYS")
    private Long totalDays = 0L;

    @Column(name = "ABSENT_DAYS")
    private Long absentDays = 0L;

    @Column(name = "OVERTIME", precision = 10, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(name = "SHORT_HOURS", precision = 10, scale = 2)
    private BigDecimal shortHours = BigDecimal.ZERO;

    @Column(name = "MEDICAL_DAYS")
    private Long medicalDays = 0L;

    @Column(name = "LEAVE_DAYS")
    private Long leaveDays = 0L;

    @Column(name = "OVERTIME_OT2", precision = 10, scale = 2)
    private BigDecimal overtimeOt2 = BigDecimal.ZERO;

    @Column(name = "WEEKLY_OFF", precision = 10, scale = 0)
    private Long weeklyOff = 0L;

    @Column(name = "GOVERNMENT_OFF", precision = 10, scale = 0)
    private Long governmentOff = 0L;

    @Column(name = "MONTHLY_WOKING_DAYS", precision = 25, scale = 0)
    private Long monthlyWokingDays;

    @Column(name = "DEDUCTDAYS")
    private Long deductDays;

    @Column(name = "EXCESS_HRS", precision = 10, scale = 2)
    private BigDecimal excessHrs;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 200)
    private String drilldownLinkInfo;

    @Column(name = "NET_HOURS")
    private BigDecimal netHours;

    @Column(name = "LATE_COUNTS")
    private Long lateCounts;

    @Column(name = "LATE_DEDUCTION")
    private BigDecimal lateDeduction;

    @Column(name = "SHORT_HOURS_DED")
    private BigDecimal shortHoursDed;
}
