package com.asg.hr.departmentmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface HrDepartmentMasterService {

    Map<String, Object> getAllDepartmentsWithFilters(String documentId, FilterRequestDto filters, Pageable pageable);

    HrDepartmentMasterResponse getDepartmentById(Long deptPoid);

    HrDepartmentMasterResponse createDepartment(HrDepartmentMasterRequest request, Long groupPoid, String userId);

    HrDepartmentMasterResponse updateDepartment(Long deptPoid, HrDepartmentMasterRequest request, Long groupPoid, String userId);

    void deleteDepartment(Long deptPoid, Long groupPoid, String userId, @Valid DeleteReasonDto deleteReasonDto);
}

