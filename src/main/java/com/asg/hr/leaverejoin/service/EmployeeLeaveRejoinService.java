package com.asg.hr.leaverejoin.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinRequest;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinResponse;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface EmployeeLeaveRejoinService {

    Map<String, Object> list(FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    EmployeeLeaveRejoinResponse getById(Long transactionPoid);

    EmployeeLeaveRejoinResponse create(EmployeeLeaveRejoinRequest request);

    EmployeeLeaveRejoinResponse update(Long transactionPoid, EmployeeLeaveRejoinRequest request);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    EmployeeLeaveRejoinEmployeeDetailsResponse getEmployeeDetails(Long employeePoid);

    EmployeeLeaveRejoinLeaveDetailsResponse getLeaveDetails(Long employeePoid, Long leaveRequestPoid);

    byte[] print(Long transactionPoid) throws JRException;
}
