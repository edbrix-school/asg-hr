package com.asg.hr.resignation.repository;

import com.asg.hr.resignation.entity.HrResignationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrResignationRepository extends JpaRepository<HrResignationEntity, Long> {

    Optional<HrResignationEntity> findByTransactionPoidAndDeletedNot(Long transactionPoid, String deleted);
}

