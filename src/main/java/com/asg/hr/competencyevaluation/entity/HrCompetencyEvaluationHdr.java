package com.asg.hr.competencyevaluation.entity;



import com.asg.common.lib.annotation.AuditIgnore;

import com.asg.common.lib.entity.BaseEntity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;

import jakarta.persistence.PrePersist;

import jakarta.persistence.Table;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;



import java.math.BigDecimal;

import java.time.LocalDate;



@Entity

@Table(name = "HR_COMPETENCY_EVALUATION_HDR")

@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class HrCompetencyEvaluationHdr extends BaseEntity {



    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "TRANSACTION_POID", nullable = false)

    @AuditIgnore

    private Long transactionPoid;



    @Column(name = "TRANSACTION_DATE", nullable = false)

    private LocalDate transactionDate;



    @Column(name = "GROUP_POID")

    private Long groupPoid;



    @Column(name = "DOC_REF", length = 25, nullable = false)

    private String docRef;



    @Column(name = "COMPANY_POID")

    private Long companyPoid;



    @Column(name = "EVALUATION_DATE")

    private LocalDate evaluationDate;



    @Column(name = "EMPLOYEE_POID")

    private Long employeePoid;



    @Column(name = "TOTAL_RATING")

    private BigDecimal totalRating;



    @Column(name = "AVG_RATING")

    private BigDecimal avgRatingPercent;



    @Column(name = "EMPLOYEE_COMMENTS", length = 2000)

    private String employeeRemarks;



    @Column(name = "TRAINING_NEEDS", length = 500)

    private String trainingNeeds;



    @Column(name = "REVIEWED_BY_POID")

    private Long reviewedByPoid;



    @Column(name = "REVIEWER_COMMENTS", length = 500)

    private String reviewerComments;



    @Column(name = "DELETED", length = 1)

    @AuditIgnore

    private String deleted;



    @Column(name = "STATUS", length = 50)

    private String status;



    @Column(name = "COMP_SCHEDULE_POID")

    private Long compSchedulePoid;



    @Column(name = "HOD_COMMENTS", length = 2000)

    private String hodRemarks;



    @Column(name = "DEPARTMENT_POID")

    private Long departmentPoid;



    @Column(name = "DESIGNATION_POID")

    private Long designationPoid;



    @Column(name = "EMPLOYEE_AGREED_PERCENT")

    private BigDecimal employeeAgreedPercent;



    @PrePersist

    void prePersist() {

        if (deleted == null) {

            deleted = "N";

        }

        if (transactionDate == null) {

            transactionDate = LocalDate.now();

        }

    }

}


