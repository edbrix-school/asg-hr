package com.asg.hr.locationmaster.repository;


import com.asg.hr.locationmaster.entity.GlobalLocationMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalLocationMasterRepository extends JpaRepository<GlobalLocationMaster, Long> {

    boolean existsByLocationPoid(Long locationPoid);

}