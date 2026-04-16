package com.asg.hr.employeetraining.repository;

import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailEntity;
import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeTrainingDetailRepository extends JpaRepository<EmployeeTrainingDetailEntity, EmployeeTrainingDetailId> {

    List<EmployeeTrainingDetailEntity> findByIdTransactionPoidOrderByIdDetRowIdAsc(Long transactionPoid);

}
