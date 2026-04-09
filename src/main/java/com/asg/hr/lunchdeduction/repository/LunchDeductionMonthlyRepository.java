package com.asg.hr.lunchdeduction.repository;

import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyHeader;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LunchDeductionMonthlyRepository extends JpaRepository<LunchDeductionMonthlyHeader, Long> {

    @EntityGraph(attributePaths = "details")
    @Query("""
            select header
            from LunchDeductionMonthlyHeader header
            where header.transactionPoid = :transactionPoid
              and (header.deleted is null or upper(header.deleted) <> upper(:deleted))
            """)
    Optional<LunchDeductionMonthlyHeader> findDetailedByTransactionPoidAndDeletedNot(
            @Param("transactionPoid") Long transactionPoid,
            @Param("deleted") String deleted
    );
}