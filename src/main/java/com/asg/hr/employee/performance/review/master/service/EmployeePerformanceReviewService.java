package com.asg.hr.employee.performance.review.master.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.employee.performance.review.master.dto.EmployeePerformanceReviewRequestDto;
import com.asg.hr.employee.performance.review.master.dto.EmployeePerformanceReviewResponseDto;

import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface EmployeePerformanceReviewService {

    EmployeePerformanceReviewResponseDto create(EmployeePerformanceReviewRequestDto requestDto);

    Map<String, Object> list(String docId, FilterRequestDto filters, Pageable pageable);

    EmployeePerformanceReviewResponseDto getById(Long id);

    EmployeePerformanceReviewResponseDto update(Long competencyPoid, EmployeePerformanceReviewRequestDto requestDto);

    void delete(Long id, DeleteReasonDto deleteReasonDto);
}
