package com.asg.hr.attendancemonthly.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.attendancemonthly.dto.*;
import org.springframework.data.domain.Pageable;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

public interface HrAttendanceMonthlyService {
    HrAttendanceMonthlyResponse getAttendanceSummary(Long transactionPoid);

    Map<String, Object> getAllAttendanceWithFilters(String documentId, FilterRequestDto filterRequest, Pageable pageable);

    void deleteAttendance(Long id, DeleteReasonDto deleteReasonDto);

    HrAttendanceMonthlyResponse saveAttendance(HrAttendanceMonthlyRequest request);

    HrAttendanceMonthlyResponse updateAttendance(Long transactionPoid, HrAttendanceMonthlyUpdateRequest request);

    HrAttendanceMonthlyLoadAttendanceDto loadAndProcessAttendance(Long transactionPoid, String lateDeductionCheck);

    void unloadAttendance(Long transactionPoid);

    HrAttendanceMonthlyDateParams calculateDateParams(LocalDate fromDate);

    String uploadOtExcel(MultipartFile file);
}
