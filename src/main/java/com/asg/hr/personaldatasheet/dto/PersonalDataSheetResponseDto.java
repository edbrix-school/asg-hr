package com.asg.hr.personaldatasheet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalDataSheetResponseDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    private String docRef;
    private Long employeePoid;
    private String employeeNamePassport;
    private String employeeNameCpr;
    private String residentStatus;

    // Address fields
    private String currentFlat;
    private String currentBldg;
    private String currentRoad;
    private String currentBlock;
    private String currentArea;
    private String currentPoBox;
    private String currentHomeTel;
    private String currentMobile;
    private String personalEmail;
    private String permanentAddress;
    private String postalAddress;

    // Read-only fields from Employee Master
    private String passportNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportExpiryDt;

    private String cprNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cprExpiryDt;

    private String remarks;
    private String status;
    private String deleted;
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    private String lastmodifiedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastmodifiedDate;

    private List<DependentResponseDto> dependents;
    private List<EmergencyContactResponseDto> emergencyContacts;
    private List<NomineeResponseDto> nominees;
    private List<PolicyResponseDto> policies;

    @Data
    public static class DependentResponseDto {
        private Long detRowId;
        private String namePassport;
        private String relation;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateOfBirth;

        private String nationality;
        private String passportNo;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate ppExpiryDate;

        private String cprNo;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate cprExpiry;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate rpExpiry;

        private String visaSponsor;
        private String mobile;
        private String telephone;
        private String remarks;
    }

    @Data
    public static class EmergencyContactResponseDto {
        private Long detRowId;
        private String name;
        private String relation;
        private String mobile;
        private String homeTel;
        private String email;
        private String country;
        private String remarks;
    }

    @Data
    public static class NomineeResponseDto {
        private Long detRowId;
        private String nomineeType;
        private BigDecimal percentage;
        private String nomineeName;
        private String relation;
        private String mobile;
        private String address;
        private String bankDetails;
        private String remarks;
    }

    @Data
    public static class PolicyResponseDto {
        private Long detRowId;
        private Long docPoid;
        private String docName;
        private String drilldownLinkInfo;
        private String policyAccepted;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate policyAcceptedOn;
    }
}