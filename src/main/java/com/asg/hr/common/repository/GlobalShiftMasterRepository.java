package com.asg.hr.common.repository;


import com.asg.hr.common.entity.GlobalShiftMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalShiftMasterRepository extends JpaRepository<GlobalShiftMaster, Long> {

    boolean existsByShiftPoid(Long shiftPoid);

}