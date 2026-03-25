package com.asg.hr.employeemaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.employeemaster.dto.EmployeeMasterRequestDto;
import com.asg.hr.employeemaster.dto.EmployeeMasterResponseDto;
import com.asg.hr.employeemaster.dto.EmployeePhotoUpdateResponseDto;
import com.asg.hr.employeemaster.dto.EmployeePhotoUpdateRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface EmployeeMasterService {

    Map<String, Object> listEmployees(String docId, FilterRequestDto filters, Pageable pageable);

    EmployeeMasterResponseDto getEmployeeById(Long employeePoid);

    EmployeeMasterResponseDto createEmployee(EmployeeMasterRequestDto requestDto);

    EmployeeMasterResponseDto updateEmployee(Long employeePoid, EmployeeMasterRequestDto requestDto);

    void deleteEmployee(Long employeePoid, DeleteReasonDto deleteReasonDto);

    EmployeePhotoUpdateResponseDto updateEmployeePhoto(Long employeePoid, EmployeePhotoUpdateRequestDto requestDto);
}

