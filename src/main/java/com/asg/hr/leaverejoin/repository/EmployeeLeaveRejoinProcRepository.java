package com.asg.hr.leaverejoin.repository;

import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;

public interface EmployeeLeaveRejoinProcRepository {

    EmployeeLeaveRejoinEmployeeDetailsResponse getEmployeeDetails(Long employeePoid);

    EmployeeLeaveRejoinLeaveDetailsResponse getLeaveDetails(Long employeePoid, Long leaveRequestPoid);
}
