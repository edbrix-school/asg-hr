package com.asg.hr.religion.repository;

import com.asg.hr.religion.entity.HrReligionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReligionRepository extends JpaRepository<HrReligionMaster, Long> {

    @Query("""
            SELECT h 
            FROM HrReligionMaster h
            WHERE h.religionPoid = :id
            AND (h.deleted = 'N' OR h.deleted IS NULL)
            """)
    Optional<HrReligionMaster> findByReligionPoidDeleted(Long id);

    Optional<HrReligionMaster> findByReligionCode(String religionCode);

    Optional<HrReligionMaster> findByReligionDescription(String description);
}
