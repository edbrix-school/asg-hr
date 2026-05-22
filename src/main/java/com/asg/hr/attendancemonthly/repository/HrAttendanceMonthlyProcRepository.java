package com.asg.hr.attendancemonthly.repository;

import java.time.LocalDate;

public interface HrAttendanceMonthlyProcRepository {
    String loadAttendance(Long transactionPoid, Long userPoid, LocalDate fromDate, LocalDate toDate, Long employeePoid, String lateDeductionCheck);

    String unloadAttendance(Long transactionPoid);
}
