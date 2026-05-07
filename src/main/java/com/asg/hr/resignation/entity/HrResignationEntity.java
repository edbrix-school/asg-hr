package com.asg.hr.resignation.entity;

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

import java.time.LocalDate;

@Entity
@Table(name = "HR_EMP_RESIGNATION_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrResignationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "DEPARTMENT_POID")
    private Long departmentPoid;

    @Column(name = "LAST_DATE_OF_WORK")
    private LocalDate lastDateOfWork;

    @Column(name = "RESIGNATION_DETAILS", length = 1000)
    private String resignationDetails;

    @Column(name = "DESIGNATION_POID")
    private Long designationPoid;

    @Column(name = "JOIN_DATE")
    private LocalDate joinDate;

    @Column(name = "HOD_REMARKS", length = 1000)
    private String hodRemarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "DIRECT_SUPERVISOR_POID")
    private Long directSupervisorPoid;

    @Column(name = "RP_EXPIRY_DATE")
    private LocalDate rpExpiryDate;

    @Column(name = "RESIGNATION_TYPE", length = 100)
    private String resignationType;

}

