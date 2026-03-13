package com.asg.hr.departmentmaster.repository;

import com.asg.hr.departmentmaster.entity.GlCostCenterMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlCostCenterMasterRepository extends JpaRepository<GlCostCenterMaster, Long> {
    boolean existsByCostCenterPoid(Long costCenterPoid);
}