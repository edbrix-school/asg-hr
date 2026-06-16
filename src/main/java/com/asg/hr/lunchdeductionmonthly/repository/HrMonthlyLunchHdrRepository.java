package com.asg.hr.lunchdeductionmonthly.repository;

import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface HrMonthlyLunchHdrRepository extends JpaRepository<HrMonthlyLunchHdr, Long> {

    boolean existsByPayrollMonthAndDeletedAndCompanyPoid(LocalDate payrollMonth, String deleted, Long companyPoid);
}
