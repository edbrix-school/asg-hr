package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDepndtsLmraDtlsResponseDto {

    private Long employeePoid;
    private Long detRowId;

    private String expatCpr;
    private String expatPp;
    private String nationality;
    private String primaryCpr;
    private String wpType;
    private Integer permitMonths;
    private String expatName;
    private String expatGender;

    private LocalDate wpExpiryDate;
    private LocalDate ppExpiryDate;
    private String expatCurrentStatus;
    private String wpStatus;
    private String inOutStatus;
    private String offenseClassification;
    private String offenceCode;
    private String offenceDescription;
    private String intention;
    private String allowMobility;
    private String mobilityInProgress;
    private String rpCancelled;
    private String rpCancellationReason;

    private String photo;
    private String signature;
    private String fingerPrint;
    private String healthCheckResult;
    private String additionalBhPermit;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

