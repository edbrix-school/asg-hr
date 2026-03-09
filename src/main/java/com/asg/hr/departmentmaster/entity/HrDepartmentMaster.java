package com.asg.hr.departmentmaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "HR_DEPARTMENT_MASTER", 
       uniqueConstraints = {
           @UniqueConstraint(name = "HR_DEPARTMENT_MASTER_UK_DPCD", columnNames = "DEPT_CODE"),
           @UniqueConstraint(name = "HR_DEPARTMENT_MASTER_UK_DPNAME", columnNames = "DEPT_NAME")
       })
public class HrDepartmentMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEPT_POID", nullable = false)
    private Long deptPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "BASE_GROUP", length = 30)
    private String baseGroup;

    @Column(name = "DEPT_CODE", length = 20, nullable = false)
    private String deptCode;

    @Column(name = "DEPT_NAME", length = 100, nullable = false)
    private String deptName;

    @Column(name = "SUBDEPT_Y_N", length = 1)
    private String subdeptYN = "N";

    @Column(name = "ACTIVE", length = 1)
    private String active = "Y";

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "PARENT_DEPT_POID")
    private Long parentDeptPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted = "N";

    @Column(name = "COST_CENTRE_POID")
    private Long costCentrePoid;
}