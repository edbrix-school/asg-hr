package com.asg.hr.attendencerequest.repository;

import com.asg.hr.attendencerequest.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    Optional<AttendanceEntity> findByAttendancePoidAndGroupPoidAndDeleted(
            Long attendancePoid,
            Long groupPoid,
            String deleted
    );

    @Modifying
    @Query(value = "INSERT INTO HR_ATTENDANCE_SPECIAL_REQ " +
            "(GROUP_POID, EMPLOYEE_POID, ATTENDANCE_DATE, EXCEPTION_TYPE, EXCEPTION_REASON, " +
            "HOD_REMARKS, OT1_HOURS, OT2_HOURS, STATUS, DELETED, TRANSACTION_DATE, " +
            "CREATED_BY, CREATED_DATE, LASTMODIFIED_BY, LASTMODIFIED_DATE) " +
            "VALUES (:groupPoid, :employeePoid, :attendanceDate, :exceptionType, :reason, " +
            ":hodRemarks, :ot1Hours, :ot2Hours, :status, :deleted, SYSDATE, " +
            ":createdBy, SYSDATE, :createdBy, SYSDATE)",
            nativeQuery = true)
    void insertAttendance(@Param("groupPoid") Long groupPoid,
                          @Param("employeePoid") Long employeePoid,
                          @Param("attendanceDate") LocalDate attendanceDate,
                          @Param("exceptionType") String exceptionType,
                          @Param("reason") String reason,
                          @Param("hodRemarks") String hodRemarks,
                          @Param("ot1Hours") String ot1Hours,
                          @Param("ot2Hours") String ot2Hours,
                          @Param("status") String status,
                          @Param("deleted") String deleted,
                          @Param("createdBy") String createdBy);

    @Query(value = "SELECT TRANSACTION_POID FROM HR_ATTENDANCE_SPECIAL_REQ " +
            "WHERE EMPLOYEE_POID = :employeePoid AND TRUNC(ATTENDANCE_DATE) = TRUNC(:attendanceDate) " +
            "AND DELETED = 'N' ORDER BY TRANSACTION_POID DESC FETCH FIRST 1 ROWS ONLY",
            nativeQuery = true)
    Long findLastInsertedPoid(@Param("employeePoid") Long employeePoid,
                              @Param("attendanceDate") LocalDate attendanceDate);
}