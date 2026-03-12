package com.asg.hr.competency.repository;

import com.asg.hr.competency.entity.CompetencyMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetencyMasterRepository extends JpaRepository<CompetencyMasterEntity, Long> {

    @Query("SELECT e FROM CompetencyMasterEntity e WHERE e.competencyPoid = :id AND e.groupPoid = :groupPoid AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    Optional<CompetencyMasterEntity> findByIdAndGroupPoidAndNotDeleted(@Param("id") Long id, @Param("groupPoid") Long groupPoid);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM CompetencyMasterEntity e " +
            "WHERE e.competencyCode = :code AND e.groupPoid = :groupPoid AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    boolean existsByCompetencyCodeAndGroupPoid(@Param("code") String code, @Param("groupPoid") Long groupPoid);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM CompetencyMasterEntity e " +
            "WHERE e.competencyCode = :code AND e.groupPoid = :groupPoid AND e.competencyPoid != :id AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    boolean existsByCompetencyCodeAndGroupPoidAndIdNot(@Param("code") String code, @Param("groupPoid") Long groupPoid, @Param("id") Long id);
}
