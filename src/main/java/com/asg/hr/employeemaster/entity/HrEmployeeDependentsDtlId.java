package com.asg.hr.employeemaster.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HrEmployeeDependentsDtlId implements Serializable {
    private Long employeePoid;
    private Long detRowId;
}