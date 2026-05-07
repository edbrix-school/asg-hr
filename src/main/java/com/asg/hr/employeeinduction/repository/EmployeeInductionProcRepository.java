package com.asg.hr.employeeinduction.repository;

import java.util.List;
import java.util.Map;

public interface EmployeeInductionProcRepository {
    
    List<Map<String, Object>> getInductionCategories();
}