package com.asg.hr.competencyevaluation.repository;

import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrCompetencyEvaluationHdrRepository extends JpaRepository<HrCompetencyEvaluationHdr, Long> {

    @Query("SELECT h FROM HrCompetencyEvaluationHdr h WHERE h.transactionPoid = :id AND h.deleted = 'N'")
    Optional<HrCompetencyEvaluationHdr> findActiveById(@Param("id") Long id);
}
