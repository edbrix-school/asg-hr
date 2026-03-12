package com.asg.hr.religion.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "HR_RELIGION_MASTER")
@Getter
@Setter
public class HrReligionMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RELIGION_POID")
    @AuditIgnore
    private Long religionPoid;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "RELIGION_CODE", unique = true, length = 20)
    private String religionCode;

    @Column(name = "RELIGION_DESCRIPTION", unique = true, length = 100)
    private String religionDescription;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

}