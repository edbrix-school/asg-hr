package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.entity.HrEmployeeDocumentDtl;
import com.asg.hr.employeemaster.entity.HrEmployeeDocumentDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrEmployeeDocumentDtlRepository extends JpaRepository<HrEmployeeDocumentDtl, HrEmployeeDocumentDtlId> {

    List<HrEmployeeDocumentDtl> findByEmployeePoid(Long employeePoid);
}