package com.asg.hr.locationmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.locationmaster.dto.LocationMasterRequestDto;
import com.asg.hr.locationmaster.dto.LocationMasterResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface LocationMasterService {

    LocationMasterResponseDto create(LocationMasterRequestDto requestDto);

    Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable);

    LocationMasterResponseDto getById(Long locationPoid);

    LocationMasterResponseDto update(Long locationPoid, LocationMasterRequestDto requestDto);

    void delete(Long locationPoid, DeleteReasonDto deleteReasonDto);
}