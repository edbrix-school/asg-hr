package com.asg.hr.leaverequest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.leaverequest.dto.LeaveCreateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveCalculationResponseDto;
import com.asg.hr.leaverequest.dto.LeaveHistoryUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveResponseDto;
import com.asg.hr.leaverequest.dto.LeaveTicketUpdateRequestDto;
import com.asg.hr.leaverequest.dto.LeaveUpdateRequestDto;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface HrLeaveRequestService {

    LeaveResponseDto  create(LeaveCreateRequestDto req);

    LeaveResponseDto  update(LeaveUpdateRequestDto req);

    LeaveResponseDto getById(Long transactionPoid);

    Map<String, Object> list(String documentId, FilterRequestDto filters, LocalDate startDateValue, LocalDate endDateValue, Pageable pageable);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> getEmployeeDetails(Long employeePoid);

    Map<String, Object> getEmployeeHod(Long employeePoid);

     Map<String, Object> getEligibleLeaveDays(
            Long companyId,
            Long empPoid,
            LocalDate leaveStartDate,
            Long settlementPoid
    );

     Map<String, Object> getTicketFamilyDetails(Long empPoid);

    Map<String, Object> updateLeaveHistory(
            Long tranId,
            String ticketIssueType,
            String ticketTillDate,
            String ticketIssuedCount);

    Map<String, Object> updateLeaveHistory(LeaveHistoryUpdateRequestDto request);

    Map<String, Object> cancelLeaveHistory(Long tranId);

    Map<String, Object> updateTicketDetails(LeaveTicketUpdateRequestDto request);

    LeaveCalculationResponseDto calculateLeaveDays(
            Long transactionPoid,
            Long employeePoid,
            String leaveType,
            String annualLeaveType,
            String emergencyLeaveType,
            String splLeaveTypes,
            LocalDate leaveStartDate,
            LocalDate planedRejoinDate,
            BigDecimal eligibleLeaveDays,
            String leaveDaysMethod
    );

    Map<String, Object> handleLeaveTypeChange(String leaveType, String leaveDaysMethod);

    byte[] print(Long transactionPoid) throws JRException;
}
