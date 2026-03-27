package com.asg.hr.common.repository;


import com.asg.hr.common.entity.AdminCrMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminCrMasterRepository extends JpaRepository<AdminCrMaster, Long> {

    boolean existsByCrPoid(Long crPoid);
}