package com.asg.hr.lunchdeduction.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "HR_MONTHLY_LUNCH_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchDeductionMonthlyDetail extends BaseEntity {

    @EmbeddedId
    private LunchDeductionMonthlyDetailId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("transactionPoid")
    @JoinColumn(name = "TRANSACTION_POID", nullable = false)
    private LunchDeductionMonthlyHeader header;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "DEDUCTION_TYPE", length = 30)
    private String deductionType;

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

    @Column(name = "LUNCH_DAYS")
    private Long lunchDays;

    @Column(name = "USER_ID", length = 20)
    private String userId;

    @Column(name = "USER_NAME", length = 100)
    private String userName;
}