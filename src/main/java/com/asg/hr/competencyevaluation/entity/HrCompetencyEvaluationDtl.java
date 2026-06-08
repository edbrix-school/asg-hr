package com.asg.hr.competencyevaluation.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HR_COMPETENCY_EVALUATION_DTL")
@IdClass(HrCompetencyEvaluationDtlId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrCompetencyEvaluationDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID", nullable = false)
    @AuditIgnore
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    @AuditIgnore
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private HrCompetencyEvaluationHdr header;

    @Column(name = "COMPETENCY_POID")
    private Long competencyPoid;

    @Column(name = "COMP_SCHEDULE_POID")
    private Long compSchedulePoid;

    @Column(name = "RATING", length = 30)
    private String rating;

    @Column(name = "REMARKS", length = 2000)
    private String remarks;

    @Column(name = "EMPLOYEE_COMMENTS", length = 2000)
    private String employeeComments;

    @Column(name = "EMPLOYEE_AGREED", length = 100)
    private String employeeAgreed;
}
