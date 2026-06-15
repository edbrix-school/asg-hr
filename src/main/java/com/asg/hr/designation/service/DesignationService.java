package com.asg.hr.designation.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.designation.dto.DesignationRequest;
import com.asg.hr.designation.dto.DesignationResponse;

public interface DesignationService {

    DesignationResponse getDesignationById(Long designationPoid);

    Long createDesignation(DesignationRequest request);

    DesignationResponse updateDesignation(Long designationPoid, DesignationRequest request);

    void deleteDesignation(Long designationPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> listDesignations(FilterRequestDto filterRequest, Pageable pageable);
}

