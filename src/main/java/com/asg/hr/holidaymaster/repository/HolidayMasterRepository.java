package com.asg.hr.holidaymaster.repository;

import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface HolidayMasterRepository extends JpaRepository<HolidayMasterEntity, Long> {

    @Query("""
            SELECT h
            FROM HolidayMasterEntity h
            WHERE h.holidayPoid = :holidayPoid
              AND (h.deleted <> :deleted OR h.deleted IS NULL)
            """)
    Optional<HolidayMasterEntity> findByHolidayPoidAndDeletedNot(@Param("holidayPoid") Long holidayPoid,
                                                                 @Param("deleted") String deleted);

    boolean existsByHolidayDate(LocalDate holidayDate);
}

