package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.entity.HrEmpDepndtsLmraDtls;
import com.asg.hr.employeemaster.entity.HrEmpDepndtsLmraDtlsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrEmpDepndtsLmraDtlsRepository extends JpaRepository<HrEmpDepndtsLmraDtls, HrEmpDepndtsLmraDtlsId> {

    List<HrEmpDepndtsLmraDtls> findByEmployeePoid(Long employeePoid);
}