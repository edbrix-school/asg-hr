package com.asg.hr.attendencerequest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AttendanceResponseDto {

    private Long attendancePoid;
    private Long employeePoid;
    private LocalDate attendanceDate;
    private LocalDate transactionDate;
    private String docRef;
    private String exceptionType;
    private String reason;
    private String hodRemarks;
    private String status;
    private String ot1Hours;
    private String ot2Hours;

    private String createdBy;
    private String createdDate;
    private String lastModifiedBy;
    private String lastModifiedDate;
}