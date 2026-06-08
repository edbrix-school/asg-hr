package com.asg.hr.attendancemonthly.repository;

import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HrAttendanceMonthlyHdrRepository extends JpaRepository<HrAttendanceMonthlyHdr, Long> {
    
    boolean existsByAttendanceFromAndAttendanceToAndEmployeePoidAndDeleted(
            LocalDate attendanceFrom, LocalDate attendanceTo, Long employeePoid, String deleted);

    boolean existsByAttendanceFromAndAttendanceToAndEmployeeWiseAndDeleted(
            LocalDate attendanceFrom, LocalDate attendanceTo, String employeeWise, String deleted);
}
