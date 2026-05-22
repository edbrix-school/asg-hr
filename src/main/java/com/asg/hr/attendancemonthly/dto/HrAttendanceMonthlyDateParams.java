package com.asg.hr.attendancemonthly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendanceMonthlyDateParams {
    private LocalDate attendanceTo;
    private String description;
}
