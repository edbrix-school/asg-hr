package com.asg.hr.common.repository;


import com.asg.hr.common.entity.GlobalFixedVariables;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalFixedVariablesRepository extends JpaRepository<GlobalFixedVariables, Long> {

    boolean existsByVariableName(String discontinuedReason);

}