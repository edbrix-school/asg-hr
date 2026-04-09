package com.asg.hr.lunchdeduction.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HR_MONTHLY_LUNCH_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchDeductionMonthlyHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "PAYROLL_MONTH")
    private LocalDate payrollMonth;

    @Column(name = "LUNCH_DESCRIPTION", length = 100)
    private String lunchDescription;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Builder.Default
    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LunchDeductionMonthlyDetail> details = new ArrayList<>();

    public void addDetail(LunchDeductionMonthlyDetail detail) {
        detail.setHeader(this);
        if (detail.getId() == null) {
            detail.setId(new LunchDeductionMonthlyDetailId());
        }
        details.add(detail);
    }

    public void removeDetail(LunchDeductionMonthlyDetail detail) {
        details.remove(detail);
        detail.setHeader(null);
    }
}