package com.asg.hr.departmentmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrDepartmentMasterResponse {

    private Long deptPoid;
    private Long groupPoid;
    private String baseGroup;
    private String deptCode;
    private String deptName;
    private String subdeptYN;
    private String active;
    private Long seqNo;
    private Long parentDeptPoid;
    private Long costCentrePoid;
    private String deleted;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
    private String lastModifiedBy;
}

