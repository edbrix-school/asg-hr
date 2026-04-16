package com.asg.hr.allowanceanddeductionmaster.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "HR_ALLOWANCE_DEDUCTION_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAllowanceDeductionMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ALLOWACE_DEDUCTION_POID", nullable = false)
    @AuditIgnore
    private Long allowaceDeductionPoid;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "CODE", length = 20, unique = true, updatable = false)
    private String code;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "VARIABLE_FIXED", length = 20)
    private String variableFixed;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "FORMULA", length = 500)
    private String formula;

    @Column(name = "GLCODE", length = 20)
    private String glcode;

    @Column(name = "MANDATORY", length = 1)
    private String mandatory;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "PAYROLL_FIELD_NAME", length = 30, unique = true)
    private String payrollFieldName;

    @PrePersist
    protected void onCreate() {
        if (deleted == null) {
            deleted = "N";
        }
        if (active == null) {
            active = "Y";
        }
    }
}
