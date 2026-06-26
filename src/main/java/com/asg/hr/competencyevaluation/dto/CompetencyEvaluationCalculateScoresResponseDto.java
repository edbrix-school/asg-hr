package com.asg.hr.competencyevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyEvaluationCalculateScoresResponseDto {

    private BigDecimal totalRating;

    private BigDecimal avgRatingPercent;

    private BigDecimal employeeAgreedPercent;

    private List<CompetencyEvaluationCalculateScoresRequestDto.DetailRatingDto> details;
}
