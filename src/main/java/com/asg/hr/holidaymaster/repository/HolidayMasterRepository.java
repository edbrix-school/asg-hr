package com.asg.hr.holidaymaster.repository;

import com.asg.hr.holidaymaster.entity.HolidayMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HolidayMasterRepository extends JpaRepository<HolidayMasterEntity, Long> {

    boolean existsByHolidayDate(LocalDate holidayDate);
}

