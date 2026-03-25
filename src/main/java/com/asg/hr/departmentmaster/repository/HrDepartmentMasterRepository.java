package com.asg.hr.departmentmaster.repository;

import com.asg.hr.departmentmaster.entity.HrDepartmentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface HrDepartmentMasterRepository extends JpaRepository<HrDepartmentMaster, Long> {
    boolean existsByDeptPoid(Long deptPoid);

    boolean existsByDeptNameIgnoreCase(String deptName);

    boolean existsByDeptNameIgnoreCaseAndDeptPoidNot(String deptName, Long deptPoid);
}