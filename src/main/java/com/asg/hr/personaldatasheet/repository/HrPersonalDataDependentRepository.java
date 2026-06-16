package com.asg.hr.personaldatasheet.repository;

import com.asg.hr.personaldatasheet.entity.HrPersonalDataDependent;
import com.asg.hr.personaldatasheet.entity.key.PersonalDataDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonalDataDependentRepository extends JpaRepository<HrPersonalDataDependent, PersonalDataDetailKey> {

    @Query("SELECT d FROM HrPersonalDataDependent d WHERE d.transactionPoid = :transactionPoid")
    List<HrPersonalDataDependent> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}