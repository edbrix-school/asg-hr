package com.asg.hr.leaverequest.repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public interface HrLeaveProcedureRepository {

    Map<String, Object> getEmployeeDetails(Long employeePoid);

    Map<String, Object> getEmployeeHod(Long employeePoid);

     Map<String, Object> getEligibleLeaveDays(
            Long companyId,
            Long empPoid,
            LocalDate leaveStartDate,
            Long settlementPoid
    );

    List<Map<String, Object>> getTicketFamilyDetails(Long empPoid);

    String updateLeaveHistory(Long tranId, String ticketIssueType, String ticketTillDate, String ticketIssuedCount);

    String updateTicketDetails(
            Long tranId,
            String ticketBookBy,
            String ticketProcessed,
            String ticketRemarks,
            BigDecimal ticketsIssued,
            String pjDocRef
    );

    Map<String, Object> validateLeave(
            Long tranId,
            LocalDate startDate,
            LocalDate endDate,
            Long empId,
            String leaveType,
            String subType,
            Long userId
    );

    String unUpdateLeaveHistory(Long tranId);

    BigDecimal getHolidayCount(LocalDate leaveStartDate, LocalDate planedRejoinDate);

    Long getLoginUserEmployeeId(Long userId);
}
