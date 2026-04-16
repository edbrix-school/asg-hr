package com.asg.hr.employeemaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.employeemaster.dto.*;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface EmployeeMasterService {

    Map<String, Object> listEmployees(String docId, FilterRequestDto filters, Pageable pageable);

    EmployeeMasterResponseDto getEmployeeById(Long employeePoid);

    EmployeeMasterResponseDto createEmployee(EmployeeMasterRequestDto requestDto);

    EmployeeMasterResponseDto updateEmployee(Long employeePoid, EmployeeMasterRequestDto requestDto);

    void deleteEmployee(Long employeePoid, DeleteReasonDto deleteReasonDto);

    EmployeePhotoUpdateResponseDto updateEmployeePhoto(Long employeePoid, EmployeePhotoUpdateRequestDto requestDto);

    EmployeeCountDto getEmployeeCounts();

    Map<String, Object> listEmployeeDashboardDetails(EmployeeDashboardListRequestDto request, Pageable pageable);

    byte[] print(Long transactionPoid) throws JRException;

    String uploadExcel(org.springframework.web.multipart.MultipartFile file);

    LmraUploadResponse uploadLmraData();

    EmployeeLeaveDatesResponseDto getEmployeeLeaveDates(Long employeePoid);

    String updateLeaveRejoin(Long employeePoid, LeaveRejoinUpdateRequestDto request);

    String removeLeaveRejoin(Long employeePoid, LeaveRejoinRemoveRequestDto request);
}

