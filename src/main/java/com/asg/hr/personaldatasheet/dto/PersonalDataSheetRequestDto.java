package com.asg.hr.personaldatasheet.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PersonalDataSheetRequestDto {

    @NotNull(message = "Employee is required")
    private Long employeePoid;

    @NotBlank(message = "Name as in Passport is required")
    private String employeeNamePassport;

    private String employeeNameCpr;

    @NotBlank(message = "Resident status is required")
    private String residentStatus;

    // Address fields
    @NotBlank(message = "Flat/Village Number is required")
    private String currentFlat;

    @NotBlank(message = "Building is required")
    private String currentBldg;

    @NotBlank(message = "Road is required")
    private String currentRoad;

    @NotBlank(message = "Block is required")
    private String currentBlock;

    @NotBlank(message = "Area is required")
    private String currentArea;

    private String currentPoBox;
    private String currentHomeTel;

    @NotBlank(message = "Mobile Number is required")
    private String currentMobile;

    @Email(message = "Invalid email format")
    private String personalEmail;

    @NotBlank(message = "Permanent Address is required")
    private String permanentAddress;

    private String postalAddress;
    private String remarks;

    @Valid
    private List<DependentDto> dependents;

    @Valid
    private List<EmergencyContactDto> emergencyContacts;

    @Valid
    private List<NomineeDto> nominees;

    @Valid
    private List<PolicyDto> policies;

    @Data
    public static class DependentDto {
        private Long detRowId;
        private String actionType; // ISCREATED, ISUPDATED, ISDELETED, NOCHANGE
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
    public static class EmergencyContactDto {
        private Long detRowId;
        private String actionType; // ISCREATED, ISUPDATED, ISDELETED, NOCHANGE
        private String name;
        private String relation;
        private String mobile;
        private String homeTel;

        @Email(message = "Invalid email format")
        private String email;

        private String country;
        private String remarks;
    }

    @Data
    public static class NomineeDto {
        private Long detRowId;
        private String actionType; // ISCREATED, ISUPDATED, ISDELETED, NOCHANGE
        private String nomineeType;
        private Double percentage;
        private String nomineeName;
        private String relation;
        private String mobile;
        private String address;
        private String bankDetails;
        private String remarks;
    }

    @Data
    public static class PolicyDto {
        private Long detRowId;
        private String actionType; // ISCREATED, ISUPDATED, ISDELETED, NOCHANGE
        private Long docPoid;
        private String docName;
        private String drilldownLinkInfo;
        private String policyAccepted;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate policyAcceptedOn;
    }
}