package com.asg.hr.designation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asg.hr.designation.entity.HrDesignationMaster;

public interface DesignationRepository extends JpaRepository<HrDesignationMaster, Long> {

    boolean existsByDesignationCodeIgnoreCase(String designationCode);

    boolean existsByDesignationNameIgnoreCase(String designationName);

    boolean existsByDesignationCodeIgnoreCaseAndDesignationPoidNot(String designationCode, Long designationPoid);

    boolean existsByDesignationNameIgnoreCaseAndDesignationPoidNot(String designationName, Long designationPoid);

    Optional<HrDesignationMaster> findByDesignationPoidAndDeleted(Long designationPoid, String deleted);
}

