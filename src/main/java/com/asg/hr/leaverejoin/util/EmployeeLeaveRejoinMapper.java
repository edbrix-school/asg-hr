package com.asg.hr.leaverejoin.util;

import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinRequest;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinResponse;
import com.asg.hr.leaverejoin.entity.HrEmployeeRejoinHdr;
import org.springframework.stereotype.Component;

@Component
public class EmployeeLeaveRejoinMapper {

    public HrEmployeeRejoinHdr toEntity(EmployeeLeaveRejoinRequest request) {
        if (request == null) {
            return null;
        }

        return HrEmployeeRejoinHdr.builder()
                .employeePoid(request.getEmployeePoid())
                .leaveRequestPoid(request.getLeaveRequestPoid())
                .dateOfRejoining(request.getDateOfRejoining())
                .remarks(trimToNull(request.getRemarks()))
                .passportReceived(normalizePassportReceived(request.getPassportReceived()))
                .receivedBy(trimToNull(request.getReceivedBy()))
                .remarksByHr(trimToNull(request.getRemarksByHr()))
                .extraLeaveDays(request.getExtraLeaveDays())
                .extraAbsentDays(request.getExtraAbsentDays())
                .deleted(EmployeeLeaveRejoinConstants.DELETED_NO)
                .build();
    }

    public void updateEntity(HrEmployeeRejoinHdr entity, EmployeeLeaveRejoinRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setEmployeePoid(request.getEmployeePoid());
        entity.setLeaveRequestPoid(request.getLeaveRequestPoid());
        entity.setDateOfRejoining(request.getDateOfRejoining());
        entity.setRemarks(trimToNull(request.getRemarks()));
        entity.setPassportReceived(normalizePassportReceived(request.getPassportReceived()));
        entity.setReceivedBy(trimToNull(request.getReceivedBy()));
        entity.setRemarksByHr(trimToNull(request.getRemarksByHr()));
        entity.setExtraLeaveDays(request.getExtraLeaveDays());
        entity.setExtraAbsentDays(request.getExtraAbsentDays());
    }

    public EmployeeLeaveRejoinResponse toResponse(HrEmployeeRejoinHdr entity) {
        if (entity == null) {
            return null;
        }

        return EmployeeLeaveRejoinResponse.builder()
                .transactionPoid(entity.getTransactionPoid())
                .transactionDate(entity.getTransactionDate())
                .docRef(entity.getDocRef())
                .companyPoid(entity.getCompanyPoid())
                .employeePoid(entity.getEmployeePoid())
                .leaveRequestPoid(entity.getLeaveRequestPoid())
                .dateOfRejoining(entity.getDateOfRejoining())
                .remarks(entity.getRemarks())
                .passportReceived(normalizePassportReceived(entity.getPassportReceived()))
                .receivedBy(entity.getReceivedBy())
                .remarksByHr(entity.getRemarksByHr())
                .extraLeaveDays(entity.getExtraLeaveDays())
                .extraAbsentDays(entity.getExtraAbsentDays())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .modifiedBy(entity.getLastModifiedBy())
                .modifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePassportReceived(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        String normalized = trimmed.toUpperCase();
        if ("Y".equals(normalized)) {
            return "YES";
        }
        if ("N".equals(normalized)) {
            return "NO";
        }
        return normalized;
    }
}
