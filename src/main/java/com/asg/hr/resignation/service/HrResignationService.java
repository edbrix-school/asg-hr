package com.asg.hr.resignation.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.resignation.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface HrResignationService {

    Map<String, Object> listResignations(FilterRequestDto filters, Pageable pageable);

    HrResignationResponse getById(Long transactionPoid);

    HrResignationResponse create(HrResignationRequest request);

    HrResignationResponse update(Long transactionPoid, HrResignationRequest request);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    HrResignationEmployeeDetailsResponse getEmployeeDetails(Long employeePoid);

}

