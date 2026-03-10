package com.asg.hr.Employee.performance.review.master.repository;

import com.asg.hr.Employee.performance.review.master.entity.EmployeePerformanceReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeePerformanceReviewRepository extends JpaRepository<EmployeePerformanceReviewEntity, Long> {

    @Query("SELECT e FROM EmployeePerformanceReviewEntity e WHERE e.competencyPoid = :id AND e.groupPoid = :groupPoid AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    Optional<EmployeePerformanceReviewEntity> findByIdAndGroupPoidAndNotDeleted(@Param("id") Long id, @Param("groupPoid") Long groupPoid);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmployeePerformanceReviewEntity e " +
            "WHERE e.competencyCode = :code AND e.groupPoid = :groupPoid AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    boolean existsByCompetencyCodeAndGroupPoid(@Param("code") String code, @Param("groupPoid") Long groupPoid);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmployeePerformanceReviewEntity e " +
            "WHERE e.competencyCode = :code AND e.groupPoid = :groupPoid AND e.competencyPoid != :id AND (e.deleted IS NULL OR e.deleted = '' OR e.deleted = 'N')")
    boolean existsByCompetencyCodeAndGroupPoidAndIdNot(@Param("code") String code, @Param("groupPoid") Long groupPoid, @Param("id") Long id);
}
