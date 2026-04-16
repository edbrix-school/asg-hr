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
public class HrEmployeeLeaveHistoryId implements Serializable {
    private Long leaveHistPoid;
    private Long detRowId;
}