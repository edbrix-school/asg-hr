package com.asg.hr.personaldatasheet.repository;

import com.asg.hr.personaldatasheet.entity.HrPersonalDataEmergency;
import com.asg.hr.personaldatasheet.entity.key.PersonalDataDetailKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonalDataEmergencyRepository extends JpaRepository<HrPersonalDataEmergency, PersonalDataDetailKey> {

    @Query("SELECT e FROM HrPersonalDataEmergency e WHERE e.transactionPoid = :transactionPoid")
    List<HrPersonalDataEmergency> findByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}