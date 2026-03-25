package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.entity.HrEmployeeLeaveHistory;
import com.asg.hr.employeemaster.entity.HrEmployeeLeaveHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrEmployeeLeaveHistoryRepository extends JpaRepository<HrEmployeeLeaveHistory, HrEmployeeLeaveHistoryId> {

    List<HrEmployeeLeaveHistory> findByEmployeePoid(Long employeePoid);
}