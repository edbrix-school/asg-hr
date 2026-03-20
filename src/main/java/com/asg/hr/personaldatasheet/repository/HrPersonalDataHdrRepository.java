package com.asg.hr.personaldatasheet.repository;

import com.asg.hr.personaldatasheet.entity.HrPersonalDataHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrPersonalDataHdrRepository extends JpaRepository<HrPersonalDataHdr, Long> {

    @Query("SELECT h FROM HrPersonalDataHdr h WHERE h.transactionPoid = :transactionPoid AND h.deleted = 'N'")
    Optional<HrPersonalDataHdr> findByTransactionPoidAndNotDeleted(@Param("transactionPoid") Long transactionPoid);

}