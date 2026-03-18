package com.asg.hr.location.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.location.dto.LocationMasterRequestDto;
import com.asg.hr.location.dto.LocationMasterResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface LocationMasterService {
    
    LocationMasterResponseDto create(LocationMasterRequestDto requestDto);
    
    Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable);
    
    LocationMasterResponseDto getById(Long id);
    
    LocationMasterResponseDto update(Long locationPoid, LocationMasterRequestDto requestDto);
    
    void delete(Long id, DeleteReasonDto deleteReasonDto);
}