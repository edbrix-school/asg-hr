package com.asg.hr.competency.repository;

import java.time.LocalDate;

public interface CompetencyScheduleProcRepository {
    String createBatchEvaluation(Long schedulePoid, Long groupPoid, Boolean recreate, LocalDate evaluationDate);
}
