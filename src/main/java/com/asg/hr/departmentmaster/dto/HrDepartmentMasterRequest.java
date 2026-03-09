package com.asg.hr.departmentmaster.dto;

import com.asg.hr.departmentmaster.annotation.ValidSubDept;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidSubDept
public class HrDepartmentMasterRequest {

    @NotBlank(message = "Department name is mandatory")
    @Size(max = 100, message = "Department name cannot exceed 100 characters")
    private String deptName;

    @Size(max = 1, message = "Sub department flag must be a single character")
    private String subdeptYN;

    @Size(max = 1, message = "Active flag must be a single character")
    private String active;

    private Long seqNo;

    private Long parentDeptPoid;

    @NotNull(message = "Cost centre POID is mandatory")
    private Long costCentrePoid;
}

