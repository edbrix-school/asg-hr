package com.asg.hr.competencyevaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyEvaluationCalculateScoresRequestDto {

    @NotEmpty
    @Valid
    private List<DetailRatingDto> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRatingDto {

        private Long detRowId;

        private Long competencyPoid;

        private String rating;

        private String employeeAgreed;
    }
}
