package com.asg.hr.attendencerequest.util;

import com.asg.hr.attendencerequest.dto.AttendanceResponseDto;
import com.asg.hr.attendencerequest.entity.AttendanceEntity;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponseDto toResponse(AttendanceEntity e) {
        return AttendanceResponseDto.builder()
                .attendancePoid(e.getAttendancePoid())
                .employeePoid(e.getEmployeePoid())
                .attendanceDate(e.getAttendanceDate())
                .transactionDate(e.getTransactionDate())
                .docRef(e.getDocRef())
                .exceptionType(e.getExceptionType())
                .reason(e.getReason())
                .hodRemarks(e.getHodRemarks())
                .status(e.getStatus())
                .ot1Hours(e.getOt1Hours())
                .ot2Hours(e.getOt2Hours())
                .createdBy(e.getCreatedBy())
                .createdDate(e.getCreatedDate() != null ? e.getCreatedDate().toString() : null)
                .lastModifiedBy(e.getLastModifiedBy())
                .lastModifiedDate(e.getLastModifiedDate() != null ? e.getLastModifiedDate().toString() : null)
                .build();
    }
}
