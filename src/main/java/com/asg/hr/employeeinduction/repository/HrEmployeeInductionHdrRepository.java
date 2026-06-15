package com.asg.hr.employeeinduction.repository;

import com.asg.hr.employeeinduction.entity.HrEmployeeInductionHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrEmployeeInductionHdrRepository extends JpaRepository<HrEmployeeInductionHdr, Long> {

    @Query("SELECT h FROM HrEmployeeInductionHdr h WHERE h.deleted = 'N'")
    List<HrEmployeeInductionHdr> findAllActive();

    @Query("SELECT h FROM HrEmployeeInductionHdr h WHERE h.transactionPoid = :poid AND h.deleted = 'N'")
    Optional<HrEmployeeInductionHdr> findByPoidAndNotDeleted(@Param("poid") Long poid);

    @Query("SELECT h FROM HrEmployeeInductionHdr h WHERE h.employeePoid = :employeePoid AND h.deleted = 'N'")
    List<HrEmployeeInductionHdr> findByEmployeePoidAndNotDeleted(@Param("employeePoid") Long employeePoid);

    @Query("SELECT h FROM HrEmployeeInductionHdr h WHERE h.docRef = :docId AND h.deleted = 'N'")
    Optional<HrEmployeeInductionHdr> findByDocIdAndNotDeleted(@Param("docId") String docId);
}