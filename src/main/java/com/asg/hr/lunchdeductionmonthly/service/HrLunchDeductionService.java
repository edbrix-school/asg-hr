package com.asg.hr.lunchdeductionmonthly.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.lunchdeductionmonthly.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface HrLunchDeductionService {

    HrLunchDeductionResponse create(HrLunchDeductionRequest request);

    HrLunchDeductionResponse update(Long transactionPoid, HrLunchDeductionRequest request);

    HrLunchDeductionResponse getById(Long transactionPoid);

    HrLunchDeductionLoadDto loadAndProcess(Long transactionPoid);

    Map<String, Object> list(FilterRequestDto filterRequest, LocalDate startDate, LocalDate endDate, Pageable pageable);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);
}
