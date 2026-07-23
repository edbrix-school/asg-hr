package com.asg.hr.competencyevaluation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetencyEvaluationRequestDto {

    private String docRef;

    @NotNull
    private Long employeePoid;

    private Long departmentPoid;

    private Long designationPoid;

    @NotNull
    private Long reviewedByPoid;

    @NotNull
    private Long compSchedulePoid;


    private LocalDate transactionDate;

    private LocalDate evaluationDate;

    @NotNull
    private String status;

    private String hodRemarks;

    private String employeeRemarks;

    private String reviewerComments;

    private String trainingNeeds;

    @NotEmpty
    @Valid
    private List<CompetencyEvaluationDetailRequestDto> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompetencyEvaluationDetailRequestDto {

        private String actionType;

        private Long detRowId;

        @NotNull
        private Long competencyPoid;

        private String rating;

        private String hodComments;

        private String employeeAgreed;

        private String employeeComments;
    }
}
