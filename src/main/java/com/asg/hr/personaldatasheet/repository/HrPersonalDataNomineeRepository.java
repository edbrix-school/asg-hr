package com.asg.hr.personaldatasheet.repository;

import com.asg.hr.personaldatasheet.entity.HrPersonalDataNominee;
import com.asg.hr.personaldatasheet.entity.key.PersonalDataDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonalDataNomineeRepository extends JpaRepository<HrPersonalDataNominee, PersonalDataDetailKey> {

    @Query("SELECT n FROM HrPersonalDataNominee n WHERE n.transactionPoid = :transactionPoid")
    List<HrPersonalDataNominee> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}