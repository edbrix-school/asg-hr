package com.asg.hr.employeeinduction.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionResponseDto;
import com.asg.hr.employeeinduction.dto.InductionCategoryDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EmployeeInductionService {

    EmployeeInductionResponseDto createInduction(EmployeeInductionRequestDto requestDto);

    EmployeeInductionResponseDto updateInduction(Long poid, EmployeeInductionRequestDto requestDto);

    EmployeeInductionResponseDto getInductionById(Long poid);

    Map<String, Object> list(FilterRequestDto filterRequest, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Map<String, Object> getInductionsByEmployee(Long employeePoid);

    Map<String, Object> loadInductionByEmployee(Long employeePoid);

    void deleteInduction(Long poid, DeleteReasonDto deleteReasonDto);

    void sendOverdueNotifications();

    List<InductionCategoryDto> getInductionCategories();
}