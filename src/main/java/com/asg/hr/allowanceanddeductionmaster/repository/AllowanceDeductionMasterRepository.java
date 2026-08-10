package com.asg.hr.allowanceanddeductionmaster.repository;

import com.asg.hr.allowanceanddeductionmaster.entity.HrAllowanceDeductionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface   AllowanceDeductionMasterRepository extends JpaRepository<HrAllowanceDeductionMaster, Long> {

    Optional<HrAllowanceDeductionMaster> findByCodeAndDeletedNot(String code, String deleted);

    Optional<HrAllowanceDeductionMaster> findByDescriptionAndDeletedNot(String description, String deleted);

    @Query("SELECT h FROM HrAllowanceDeductionMaster h WHERE h.groupPoid = :groupPoid AND h.deleted != 'Y' ORDER BY h.seqno ASC")
    List<HrAllowanceDeductionMaster> findByGroupPoidOrderBySeqNo(@Param("groupPoid") Long groupPoid);

    @Query("SELECT h FROM HrAllowanceDeductionMaster h WHERE h.glPoid = :glPoid AND h.deleted != 'Y'")
    List<HrAllowanceDeductionMaster> findByGlPoid(@Param("glPoid") Long glPoid);


}