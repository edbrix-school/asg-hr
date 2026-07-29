package com.asg.hr.designation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.asg.hr.designation.entity.HrDesignationMaster;

public interface DesignationRepository extends JpaRepository<HrDesignationMaster, Long> {

    boolean existsByDesignationCode(String designationCode);

    boolean existsByDesignationName(String designationName);

    boolean existsByDesignationCodeAndDesignationPoidNot(String designationCode, Long designationPoid);

    boolean existsByDesignationNameAndDesignationPoidNot(String designationName, Long designationPoid);

    Optional<HrDesignationMaster> findByDesignationPoidAndDeleted(Long designationPoid, String deleted);
}

