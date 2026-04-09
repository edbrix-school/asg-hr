package com.asg.hr.attendencerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDto {

    @NotNull(message = "Employee is required")
    private Long employeePoid;

    @NotNull(message = "Attendance date is required")
    @PastOrPresent(message = "Attendance date cannot be a future date")
    private LocalDate attendanceDate;

    @NotBlank(message = "Exception type is required")
    @Size(max = 20, message = "Exception type must not exceed 20 characters")
    private String exceptionType;

    @NotBlank(message = "Reason is required")
    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;

    @Size(max = 500, message = "HOD remarks must not exceed 500 characters")
    private String hodRemarks;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;
}
