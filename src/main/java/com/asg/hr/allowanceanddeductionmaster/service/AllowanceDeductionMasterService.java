package com.asg.hr.allowanceanddeductionmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.allowanceanddeductionmaster.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface AllowanceDeductionMasterService {

    AllowanceDeductionResponseDTO create(AllowanceDeductionRequestDTO request);

    AllowanceDeductionResponseDTO update(Long allowaceDeductionPoid, AllowanceDeductionRequestDTO request);

    AllowanceDeductionResponseDTO getById(Long allowaceDeductionPoid);

    void delete(Long allowaceDeductionPoid, DeleteReasonDto deleteReasonDto);

    Map<String, Object> search(FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
