package com.asg.hr.competency.repository;

import com.asg.hr.competency.entity.HrCompetencySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HrCompetencyScheduleRepository extends JpaRepository<HrCompetencySchedule, Long> {
    
    Optional<HrCompetencySchedule> findBySchedulePoidAndDeleted(Long schedulePoid, String deleted);

    @Query("SELECT COUNT(s) > 0 FROM HrCompetencySchedule s WHERE s.groupPoid = :groupPoid " +
           "AND s.deleted = 'N' AND s.schedulePoid != :schedulePoid " +
           "AND ((s.periodFrom <= :periodTo AND s.periodTo >= :periodFrom))")
    boolean existsOverlappingPeriod(@Param("groupPoid") Long groupPoid, 
                                   @Param("periodFrom") LocalDate periodFrom,
                                   @Param("periodTo") LocalDate periodTo,
                                   @Param("schedulePoid") Long schedulePoid);
}
