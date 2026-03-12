package com.asg.hr.competency.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.competency.dto.CompetencyMasterRequestDto;
import com.asg.hr.competency.dto.CompetencyMasterResponseDto;

import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface CompetencyMasterService {

    CompetencyMasterResponseDto create(CompetencyMasterRequestDto requestDto);

    Map<String, Object> list(String docId, FilterRequestDto filters, Pageable pageable);

    CompetencyMasterResponseDto getById(Long id);

    CompetencyMasterResponseDto update(Long competencyPoid, CompetencyMasterRequestDto requestDto);

    void delete(Long id, DeleteReasonDto deleteReasonDto);
}
