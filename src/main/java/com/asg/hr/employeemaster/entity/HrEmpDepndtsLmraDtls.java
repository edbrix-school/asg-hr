package com.asg.hr.employeemaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@IdClass(HrEmpDepndtsLmraDtlsId.class)
@Table(name = "HR_EMP_DEPNDTS_LMRA_DTLS")
public class HrEmpDepndtsLmraDtls extends BaseEntity {

    @Id
    @Column(name = "EMPLOYEE_POID", nullable = false)
    private Long employeePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "EXPAT_CPR", length = 20)
    private String expatCpr;

    @Column(name = "EXPAT_PP", length = 20)
    private String expatPp;

    @Column(name = "NATIONALITY", length = 20)
    private String nationality;

    @Column(name = "PRIMARY_CPR", length = 20)
    private String primaryCpr;

    @Column(name = "WP_TYPE", length = 20)
    private String wpType;

    @Column(name = "PERMIT_MONTHS")
    private Integer permitMonths;

    @Column(name = "EXPAT_NAME", length = 100)
    private String expatName;

    @Column(name = "EXPAT_GENDER", length = 10)
    private String expatGender;

    @Column(name = "WP_EXPIRY_DATE")
    private LocalDate wpExpiryDate;

    @Column(name = "PP_EXPIRY_DATE")
    private LocalDate ppExpiryDate;

    @Column(name = "EXPAT_CURRENT_STATUS", length = 100)
    private String expatCurrentStatus;

    @Column(name = "WP_STATUS", length = 100)
    private String wpStatus;

    @Column(name = "IN_OUT_STATUS", length = 20)
    private String inOutStatus;

    @Column(name = "OFFENSE_CLASSIFICATION", length = 100)
    private String offenseClassification;

    @Column(name = "OFFENCE_CODE", length = 50)
    private String offenceCode;

    @Column(name = "OFFENCE_DESCRIPTION", length = 500)
    private String offenceDescription;

    @Column(name = "INTENTION", length = 100)
    private String intention;

    @Column(name = "ALLOW_MOBILITY", length = 10)
    private String allowMobility;

    @Column(name = "MOBILITY_IN_PROGRESS", length = 100)
    private String mobilityInProgress;

    @Column(name = "RP_CANCELLED", length = 20)
    private String rpCancelled;

    @Column(name = "RP_CANCELLATION_REASON", length = 100)
    private String rpCancellationReason;

    @Column(name = "PHOTO", length = 10)
    private String photo;

    @Column(name = "SIGNATURE", length = 10)
    private String signature;

    @Column(name = "FINGER_PRINT", length = 10)
    private String fingerPrint;

    @Column(name = "HEALTH_CHECK_RESULT", length = 100)
    private String healthCheckResult;

    @Column(name = "ADDITIONAL_BH_PERMIT", length = 50)
    private String additionalBhPermit;
}