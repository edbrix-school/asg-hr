package com.asg.hr.employeetraining.repository;

import com.asg.hr.employeetraining.entity.EmployeeTrainingHeaderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface EmployeeTrainingHeaderRepository extends JpaRepository<EmployeeTrainingHeaderEntity, Long> {

    Optional<EmployeeTrainingHeaderEntity> findByTransactionPoidAndDeletedNot(Long transactionPoid, String deleted);

    boolean existsByCourseNameIgnoreCaseAndPeriodFromAndPeriodToAndDeletedNot(
            String courseName,
            LocalDate periodFrom,
            LocalDate periodTo,
            String deleted
    );

    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN TRUE ELSE FALSE END
            FROM EmployeeTrainingHeaderEntity h
            WHERE UPPER(h.courseName) = UPPER(:courseName)
              AND h.periodFrom = :periodFrom
              AND h.periodTo = :periodTo
              AND h.deleted <> :deleted
              AND h.transactionPoid <> :excludePoid
            """)
    boolean existsDuplicateOnUpdate(
            @Param("courseName") String courseName,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo,
            @Param("deleted") String deleted,
            @Param("excludePoid") Long excludePoid
    );
}
