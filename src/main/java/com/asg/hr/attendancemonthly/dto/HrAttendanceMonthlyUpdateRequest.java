package com.asg.hr.attendancemonthly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrAttendanceMonthlyUpdateRequest {

    @NotNull(message = "Attendance From date is mandatory")
    private LocalDate attendanceFrom;

    @NotNull(message = "Attendance To date is mandatory")
    private LocalDate attendanceTo;

    @NotBlank(message = "Attendance Description is mandatory")
    private String attendanceDescription;

    @NotNull(message = "Transaction date is mandatory")
    private LocalDate transactionDate;
}
