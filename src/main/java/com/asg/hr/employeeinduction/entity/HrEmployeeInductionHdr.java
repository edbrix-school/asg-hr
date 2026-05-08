package com.asg.hr.employeeinduction.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "HR_EMPLOYEE_INDUCTION_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrEmployeeInductionHdr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    @AuditIgnore
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 30)
    private String docRef;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HrEmployeeInductionDtl> details;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
    }

    // Backward compatibility methods
    public Long getPoid() {
        return transactionPoid;
    }

    public void setPoid(Long poid) {
        this.transactionPoid = poid;
    }

    public String getDocId() {
        return docRef;
    }

    public void setDocId(String docId) {
        this.docRef = docId;
    }
}