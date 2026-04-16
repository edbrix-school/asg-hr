package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeMasterResponseDto {

    private Long employeePoid;
    private Long groupPoid;

    private String employeeCode;
    private String employeeName;
    private String employeeName2;
    private String baseGroup;

    private Long companyPoid;
    private Long locationPoid;
    private Long departmentPoid;
    private Long designationPoid;

    private byte[] photo;

    private LocalDate joinDate;
    private Long nationalityPoid;
    private String gender;
    private String maritalStatus;
    private Long religionPoid;
    private LocalDate dateOfBirth;
    private String presentAddress;
    private String permanentAddress;
    private String postalAddress;
    private String homeCountryPhone;

    private String mobile;
    private String personalEmail;
    private String businessEmail;
    private String bloodGroup;
    private String emergencyContactPerson;
    private String emergencyContactNo;

    private LocalDate serviceStartDate;
    private String serviceType;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private String probation;
    private String noticePeriod;

    private Long directSupervisorPoid;
    private Long loginUserPoid;
    private String jobDescription;
    private Long airSectorPoid;
    private String ticketPeriod;
    private String noOfTickets;
    private String accessCardIssued;
    private String discontinued;
    private LocalDate discontinuedDate;
    private String reason;
    private String otherReasons;

    private BigDecimal basicSalary;
    private BigDecimal netSalary;
    private Long currencyPoid;
    private String paymentMethod;

    private Long bankPoid;
    private String accountNo;
    private String iban;
    private BigDecimal registeredSalary;
    private LocalDate lastIncrementDate;
    private LocalDate nextIncrementDate;
    private BigDecimal holdSalary;
    private String holdReason;

    private Long crPoid;
    private String passportNo;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String placeOfIssue;
    private String passportPossessedBy;
    private String gosiNo;
    private BigDecimal gosiReturnAmount;
    private String cprNo;
    private LocalDate cprExpiryDate;
    private String cprOccupation;
    private String medicalInsurance;
    private LocalDate medicalInsuranceExpiryDate;
    private String rpNo;
    private LocalDate rpStartDate;
    private LocalDate rpExpiryDate;

    private String active;
    private Integer seqNo;
    private String employeeBiomatrixId;
    private String otApplicable;
    private String insuranceNominee;
    private String insuranceNomineeRelation;
    private Long shiftPoid;
    private String currencyCode;
    private String deleted;
    private LocalDate drivingLicExp;
    private String nomineeContactDtl;
    private String displayName;
    private Long empGlPoid;
    private String insuNominee2;
    private String bankRegisterEmployeeNo;
    private String probationBenefits;
    private LocalDate probationCompletedOn;
    private String lifeInsurance;
    private String attendanceFromLog;
    private String managementStaff;
    private String extNo;
    private String attendanceLateCheck;
    private String attendanceLunchCheck;
    private String passportRemarks;
    private String placeOfHire;
    private Long pettyCashGlPoid;
    private String attendanceCheck;
    private String recruitmentSource;
    private String recruitmentDetails;
    private String recruitmentBudgeted;
    private String recruitmentReason;
    private BigDecimal ot1Limit;
    private BigDecimal ot2Limit;
    private String shortHours;
    private String codeOfConductSigned;
    private String orientation;
    private String orientationRemarks;
    private String freeLunch;
    private String staffAccommodation;
    private LocalDate actualDob;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private List<EmployeeDependentsDtlResponseDto> dependentsDetails;
    private List<EmployeeDepndtsLmraDtlsResponseDto> lmraDetails;
    private List<EmployeeExperienceDtlResponseDto> experienceDetails;
    private List<EmployeeDocumentDtlResponseDto> documentDetails;
    private List<EmployeeLeaveHistoryResponseDto> leaveHistoryDetails;
}

