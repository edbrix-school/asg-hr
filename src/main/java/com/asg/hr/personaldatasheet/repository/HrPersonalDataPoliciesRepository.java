package com.asg.hr.personaldatasheet.repository;

import com.asg.hr.personaldatasheet.entity.HrPersonalDataPolicies;
import com.asg.hr.personaldatasheet.entity.key.PersonalDataDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonalDataPoliciesRepository extends JpaRepository<HrPersonalDataPolicies, PersonalDataDetailKey> {

    @Query("SELECT p FROM HrPersonalDataPolicies p WHERE p.transactionPoid = :transactionPoid")
    List<HrPersonalDataPolicies> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}