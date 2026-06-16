package com.asg.hr.lunchdeductionmonthly.entity;

import com.asg.common.lib.entity.BaseEntity;
import com.asg.hr.lunchdeductionmonthly.entity.key.HrMonthlyLunchDtlKey;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "HR_MONTHLY_LUNCH_DTL")
@IdClass(HrMonthlyLunchDtlKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrMonthlyLunchDtl extends BaseEntity {

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "DEDUCTION_TYPE", length = 30)
    private String deductionType;

    @Column(name = "USER_ID", length = 20)
    private String userId;

    @Column(name = "USER_NAME", length = 100)
    private String userName;

    @Column(name = "LUNCH_DAYS")
    private Long lunchDays;

    @Column(name = "MONTH_DAYS")
    private Long monthDays;

    @Column(name = "OFF_DAYS")
    private Long offDays;

    @Column(name = "TOTAL_DAYS")
    private Long totalDays;

    @Column(name = "COST_PER_DAY", precision = 38, scale = 1)
    private BigDecimal costPerDay;

    @Column(name = "LUNCH_DEDCTION_AMT", precision = 38, scale = 1)
    private BigDecimal lunchDeductionAmt;

    @Column(name = "REMARKS", length = 200)
    private String remarks;
}
