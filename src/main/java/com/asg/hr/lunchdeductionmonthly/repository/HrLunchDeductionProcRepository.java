package com.asg.hr.lunchdeductionmonthly.repository;

import java.time.LocalDate;

public interface HrLunchDeductionProcRepository {

    String loadLunchDetails(Long transactionPoid, Long userPoid, LocalDate payrollMonth);
}
