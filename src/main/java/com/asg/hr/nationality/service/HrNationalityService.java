package com.asg.hr.nationality.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.nationality.dto.request.HrNationalityRequest;
import com.asg.hr.nationality.dto.request.HrNationalityUpdateRequest;
import com.asg.hr.nationality.dto.response.HrNationalityResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface HrNationalityService {

    HrNationalityResponse create(HrNationalityRequest request);

    HrNationalityResponse update(Long nationPoid, HrNationalityUpdateRequest request);

    HrNationalityResponse getById(Long nationPoid);

    void delete(Long nationPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> list(FilterRequestDto filters, Pageable pageable);
}
