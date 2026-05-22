package com.asg.hr.attendancemonthly.repository;

import com.asg.hr.attendancemonthly.entity.HrAttendanceMonthlyDtl;
import com.asg.hr.attendancemonthly.entity.key.HrAttendanceMonthlyDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrAttendanceMonthlyDtlRepository extends JpaRepository<HrAttendanceMonthlyDtl, HrAttendanceMonthlyDtlKey> {
    List<HrAttendanceMonthlyDtl> findByTransactionPoid(Long transactionPoid);
}
