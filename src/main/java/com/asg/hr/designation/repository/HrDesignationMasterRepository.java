package com.asg.hr.designation.repository;

import com.asg.hr.designation.entity.HrDesignationMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrDesignationMasterRepository extends JpaRepository<HrDesignationMaster, Long> {
    boolean existsByDesigPoid(Long desigPoid);
}