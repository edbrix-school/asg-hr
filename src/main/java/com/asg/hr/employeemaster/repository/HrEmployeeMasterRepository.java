package com.asg.hr.employeemaster.repository;

import com.asg.hr.employeemaster.entity.HrEmployeeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrEmployeeMasterRepository extends JpaRepository<HrEmployeeMaster, Long> {
    Optional<HrEmployeeMaster> findByEmployeePoid(@Param("employeePoid") Long employeePoid);

    boolean existsByEmployeePoid(@Param("employeePoid") Long employeePoid);

    boolean existsByMobile(@Param("mobile") String mobile);

    boolean existsByMobileAndEmployeePoidNot(@Param("mobile") String mobile, @Param("employeePoid") Long employeePoid);

    boolean existsByEmployeeName(@Param("employeeName") String employeeName);

    boolean existsByEmployeeNameAndEmployeePoidNot(@Param("employeeName") String employeeName, @Param("employeePoid") Long employeePoid);

    boolean existsByDirectSupervisorPoid(@Param("employeePoid") Long employeePoid);
}

