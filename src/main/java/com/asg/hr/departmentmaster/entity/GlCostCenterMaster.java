package com.asg.hr.departmentmaster.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "GL_COST_CENTER_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "GL_COST_CENTER_UK_CODE", columnNames = "COST_CENTER_CODE"),
                @UniqueConstraint(name = "GL_COST_CENTER_UK_DESC", columnNames = "COST_CENTER_DESCRIPTION")
        }
)
public class GlCostCenterMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "costCenterSeq")
    @SequenceGenerator(
            name = "costCenterSeq",
            sequenceName = "GL_COST_CENTER_MASTER_SEQ",
            allocationSize = 1
    )
    @Column(name = "COST_CENTER_POID", nullable = false)
    private Long costCenterPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COST_CENTER_CODE", length = 20)
    private String costCenterCode;

    @Column(name = "COST_CENTER_DESCRIPTION", length = 100)
    private String costCenterDescription;

    @Column(name = "COST_CENTER_DESCRIPTION2", length = 100)
    private String costCenterDescription2;

    @Column(name = "COST_CENTER_LEVEL")
    private Long costCenterLevel;

    @Column(name = "COST_CENTER_GROUP", length = 20)
    private String costCenterGroup;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "COST_CENTER_GROUP_TYPE")
    private Long costCenterGroupType;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "COST_GROUP_LIST", length = 500)
    private String costGroupList;

    @Column(name = "COST_CODE_LIST", length = 500)
    private String costCodeList;

    @Column(name = "MIS_GROUP", length = 100)
    private String misGroup;

    @Column(name = "COST_CENTER_CHILD", length = 1)
    private String costCenterChild = "N";

    @Column(name = "COST_CENTER_TYPE", length = 20)
    private String costCenterType;

    @Column(name = "PARENT_COST_CENTER_POID")
    private Long parentCostCenterPoid;

    @Column(name = "COST_CENTER_FLAG", length = 20)
    private String costCenterFlag = "NEW";
}