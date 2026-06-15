package com.asg.hr.lunchdeductionmonthly.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "HR_MONTHLY_LUNCH_HDR")
public class HrMonthlyLunchHdr extends BaseEntity {

    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @AuditIgnore
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @AuditIgnore
    @Column(name = "DOC_REF", length = 25, unique = true, insertable = false, updatable = false)
    @Generated
    private String docRef;

    @Column(name = "PAYROLL_MONTH")
    private LocalDate payrollMonth;

    @Column(name = "LUNCH_DESCRIPTION", length = 100)
    private String description;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @AuditIgnore
    @Column(name = "DELETED", length = 1)
    private String deleted = "N";
}
