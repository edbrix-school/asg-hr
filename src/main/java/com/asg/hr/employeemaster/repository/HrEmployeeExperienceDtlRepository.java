package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.entity.HrEmployeeExperienceDtl;
import com.asg.hr.employeemaster.entity.HrEmployeeExperienceDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrEmployeeExperienceDtlRepository extends JpaRepository<HrEmployeeExperienceDtl, HrEmployeeExperienceDtlId> {

    List<HrEmployeeExperienceDtl> findByEmployeePoid(Long employeePoid);
}