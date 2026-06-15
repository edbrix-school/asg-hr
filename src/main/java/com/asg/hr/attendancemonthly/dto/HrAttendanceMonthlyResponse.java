package com.asg.hr.attendancemonthly.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendanceMonthlyResponse {
    private Long transactionPoid;
    private String docRef;
    private LocalDate attendanceFrom;
    private LocalDate attendanceTo;
    private String attendanceDescription;
    private String employeeWise;
    private Long employeePoid;
    private LocalDate transactionDate;
    private String loadedPayroll;
    private List<HrAttendanceMonthlyDtlResponse> details;
}
