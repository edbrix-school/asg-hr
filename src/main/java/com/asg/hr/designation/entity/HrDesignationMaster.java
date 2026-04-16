package com.asg.hr.designation.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "HR_DESIGNATION_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "HR_DESG_MASTER_UK_CODE", columnNames = "DESIGNATION_CODE"),
                @UniqueConstraint(name = "HR_DESG_MASTER_UK_NAME", columnNames = "DESIGNATION_NAME")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class HrDesignationMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DESIG_POID")
    @AuditIgnore
    private Long designationPoid;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "DESIGNATION_CODE", length = 20, nullable = false)
    private String designationCode;

    @Column(name = "DESIGNATION_NAME", length = 100, nullable = false)
    private String designationName;

    @Column(name = "JOB_DESCRIPTION", length = 4000)
    private String jobDescription;

    @Column(name = "SKILL_DESCRIPTION")
    private String skillDescription;

    @Column(name = "REPORTING_TO")
    private String reportingToPoid;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "ACTIVE", length = 1)
    private String active = "Y";

    @Column(name = "DELETED", length = 1)
    private String deleted = "N";
}

