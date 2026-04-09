package com.asg.hr.lunchdeduction.repository;

import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetail;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LunchDeductionMonthlyDetailRepository extends JpaRepository<LunchDeductionMonthlyDetail, LunchDeductionMonthlyDetailId> {
}