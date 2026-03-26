package com.asg.hr.designation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "HR_DESIGNATION_MASTER")
@Getter
@Setter
public class HrDesignationMaster {

    @Id
    @Column(name = "DESIG_POID")
    private Long desigPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "DESIGNATION_CODE", nullable = false, length = 20)
    private String designationCode;

    @Column(name = "DESIGNATION_NAME", nullable = false, length = 100)
    private String designationName;

    @Column(name = "JOB_DESCRIPTION", length = 4000)
    private String jobDescription;

    @Column(name = "SKILL_DESCRIPTION", length = 2500)
    private String skillDescription;

    @Column(name = "REPORTING_TO", length = 100)
    private String reportingTo;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;
}