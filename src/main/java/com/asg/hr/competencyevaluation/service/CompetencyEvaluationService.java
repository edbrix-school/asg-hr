package com.asg.hr.competencyevaluation.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationResponseDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface CompetencyEvaluationService {

    CompetencyEvaluationResponseDto create(CompetencyEvaluationRequestDto request);

    CompetencyEvaluationResponseDto update(Long transactionPoid, CompetencyEvaluationRequestDto request);

    CompetencyEvaluationResponseDto getById(Long transactionPoid);

    Map<String, Object> list(FilterRequestDto filterRequest, LocalDate startDate, LocalDate endDate, Pageable pageable);

    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    CompetencyEvaluationResponseDto calculateScores(Long transactionPoid);
}
