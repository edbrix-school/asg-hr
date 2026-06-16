package com.asg.hr.lunchdeductionmonthly.repository;

import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchDtl;
import com.asg.hr.lunchdeductionmonthly.entity.key.HrMonthlyLunchDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrMonthlyLunchDtlRepository extends JpaRepository<HrMonthlyLunchDtl, HrMonthlyLunchDtlKey> {

    List<HrMonthlyLunchDtl> findByTransactionPoid(Long transactionPoid);
}
