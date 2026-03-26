package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.entity.HrEmployeeDependentsDtl;
import com.asg.hr.employeemaster.entity.HrEmployeeDependentsDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrEmployeeDependentRepository extends JpaRepository<HrEmployeeDependentsDtl, HrEmployeeDependentsDtlId> {

    List<HrEmployeeDependentsDtl> findByEmployeePoid(Long employeePoid);

    Boolean existsByName(String name);

    boolean existsByNameAndEmployeePoidAndDetRowIdNot(String name, Long employeePoid, Long detRowId);
}