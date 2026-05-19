package com.asg.hr.competencyevaluation.repository;

import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationDtl;
import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrCompetencyEvaluationDtlRepository extends JpaRepository<HrCompetencyEvaluationDtl, HrCompetencyEvaluationDtlId> {

    @Query("SELECT d FROM HrCompetencyEvaluationDtl d WHERE d.transactionPoid = :transactionPoid ORDER BY d.detRowId")
    List<HrCompetencyEvaluationDtl> findByTransactionPoidOrderByDetRowId(@Param("transactionPoid") Long transactionPoid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM HrCompetencyEvaluationDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
