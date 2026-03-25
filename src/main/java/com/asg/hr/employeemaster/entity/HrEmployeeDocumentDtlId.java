package com.asg.hr.employeemaster.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HrEmployeeDocumentDtlId implements Serializable {
    private Long employeePoid;
    private Long detRowId;
}