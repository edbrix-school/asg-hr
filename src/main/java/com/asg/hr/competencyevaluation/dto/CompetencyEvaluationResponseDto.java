package com.asg.hr.competencyevaluation.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Data;

import lombok.NoArgsConstructor;



import java.math.BigDecimal;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.List;



@Data

@Builder

@NoArgsConstructor

@AllArgsConstructor

public class CompetencyEvaluationResponseDto {



    private Long transactionPoid;

    private String docRef;

    private LocalDate transactionDate;

    private Long employeePoid;
    private LovGetListDto employeeDet;

    private Long departmentPoid;
    private LovGetListDto departmentDet;

    private Long designationPoid;
    private LovGetListDto designationDet;

    private Long reviewedByPoid;
    private LovGetListDto reviewedByDet;

    private Long compSchedulePoid;
    private LovGetListDto compScheduleDet;

    private LocalDate evaluationDate;

    private String status;

    private String hodRemarks;

    private String employeeRemarks;

    private String reviewerComments;

    private String trainingNeeds;

    private BigDecimal totalRating;

    private BigDecimal avgRatingPercent;

    private BigDecimal employeeAgreedPercent;

    private String createdBy;

    private LocalDateTime createdDate;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;

    private List<CompetencyEvaluationDetailResponseDto> details;



    @Data

    @Builder

    @NoArgsConstructor

    @AllArgsConstructor

    public static class CompetencyEvaluationDetailResponseDto {



        private Long detRowId;

        private Long competencyPoid;
        private LovGetListDto competencyDet;

        private Long compSchedulePoid;
        private LovGetListDto compScheduleDet;

        private String rating;

        private String hodComments;

        private String employeeAgreed;

        private String employeeComments;


        private String actionType;

    }

}


