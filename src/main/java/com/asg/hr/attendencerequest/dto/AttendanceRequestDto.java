package com.asg.hr.attendencerequest.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDto {

    private Long employeePoid;
    private LocalDate attendanceDate;
    private String exceptionType;
    private String reason;
    private String hodRemarks;
    private String status;
}
