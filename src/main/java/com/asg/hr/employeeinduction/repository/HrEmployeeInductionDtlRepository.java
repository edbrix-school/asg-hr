package com.asg.hr.employeeinduction.repository;

import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtl;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrEmployeeInductionDtlRepository extends JpaRepository<HrEmployeeInductionDtl, HrEmployeeInductionDtlId> {

    @Query("SELECT d FROM HrEmployeeInductionDtl d WHERE d.transactionPoid = :hdrPoid")
    List<HrEmployeeInductionDtl> findByHdrPoidAndNotDeleted(@Param("hdrPoid") Long hdrPoid);

    @Query("SELECT d FROM HrEmployeeInductionDtl d WHERE d.sheduledDate < :currentDate AND d.status = 'N'")
    List<HrEmployeeInductionDtl> findOverdueInductions(@Param("currentDate") LocalDate currentDate);
}