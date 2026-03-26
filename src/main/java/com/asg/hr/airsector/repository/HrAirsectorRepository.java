package com.asg.hr.airsector.repository;

import com.asg.hr.airsector.entity.HrAirsectorMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrAirsectorRepository extends JpaRepository<HrAirsectorMaster, Long> {

    boolean existsByAirsectorDescription(String airsectorDescription);

    boolean existsByAirsectorDescriptionAndAirsecPoidNot(String airsectorDescription, Long airsecPoid);

    boolean existsByAirsecPoid(Long airsecPoid);
}
