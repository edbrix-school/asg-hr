package com.asg.hr.employeetraining.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "HR_EMPLOYEE_TRAINING_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTrainingHeaderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "COURSE_NAME", length = 200)
    private String courseName;

    @Column(name = "PERIOD_FROM")
    private LocalDate periodFrom;

    @Column(name = "PERIOD_TO")
    private LocalDate periodTo;

    @Column(name = "DURATION_DAYS")
    private Integer durationDays;

    @Column(name = "TRAINING_TYPE", length = 30)
    private String trainingType;

    @Column(name = "INSTITUTION", length = 100)
    private String institution;

    @Column(name = "TRAINING_COST")
    private BigDecimal trainingCost;

    @Column(name = "TRAINING_LOCATION", length = 50)
    private String trainingLocation;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "DOC_REF", length = 30)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "EMPLOYEE_POID", length = 20)
    private String employeePoid;
}
