package com.asg.hr.attendencerequest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.attendencerequest.dto.AttendanceRequestDto;
import com.asg.hr.attendencerequest.dto.AttendanceResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AttendanceSpecialService {

    AttendanceResponseDto create(AttendanceRequestDto dto);

    Map<String, Object> list(String docId, FilterRequestDto filters, Pageable pageable);

    AttendanceResponseDto getById(Long id);

    AttendanceResponseDto update(Long id, AttendanceRequestDto dto);

    void delete(Long id, DeleteReasonDto reason);
}