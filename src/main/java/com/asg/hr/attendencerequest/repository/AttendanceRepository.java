package com.asg.hr.attendencerequest.repository;

import com.asg.hr.attendencerequest.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    Optional<AttendanceEntity> findByAttendancePoidAndGroupPoidAndDeleted(
            Long attendancePoid,
            Long groupPoid,
            String deleted
    );
}