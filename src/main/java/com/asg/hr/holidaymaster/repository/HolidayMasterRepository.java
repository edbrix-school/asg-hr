package com.asg.hr.holidaymaster.repository;

import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface HolidayMasterRepository extends JpaRepository<HolidayMasterEntity, Long> {

    Optional<HolidayMasterEntity> findByHolidayPoidAndDeletedNot(Long holidayPoid, String deleted);

    boolean existsByHolidayDate(LocalDate holidayDate);
}

