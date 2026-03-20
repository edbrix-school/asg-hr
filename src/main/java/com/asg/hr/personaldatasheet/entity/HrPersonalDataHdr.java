package com.asg.hr.personaldatasheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "HR_PERSONAL_DATA_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonalDataHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF")
    private String docRef;

    @Column(name = "EMPLOYEE_POID")
    @NotNull(message = "Employee is mandatory")
    private Long employeePoid;

    @Column(name = "EMPLOYEE_NAME_PASSPORT")
    @NotBlank(message = "Name as in Passport is mandatory")
    private String employeeNamePassport;

    @Column(name = "EMPLOYEE_NAME_CPR")
    private String employeeNameCpr;

    @Column(name = "RESIDENT_STATUS")
    @NotBlank(message = "Resident Status in Bahrain is mandatory")
    private String residentStatus;

    @Column(name = "CURRENT_FLAT")
    @NotBlank(message = "Flat/Village Number is mandatory")
    private String currentFlat;

    @Column(name = "CURRENT_BLDG")
    @NotBlank(message = "Building is mandatory")
    private String currentBldg;

    @Column(name = "CURRENT_ROAD")
    @NotBlank(message = "Road is mandatory")
    private String currentRoad;

    @Column(name = "CURRENT_BLOCK")
    @NotBlank(message = "Block is mandatory")
    private String currentBlock;

    @Column(name = "CURRENT_AREA")
    @NotBlank(message = "Area is mandatory")
    private String currentArea;

    @Column(name = "CURRENT_PO_BOX")
    private String currentPoBox;

    @Column(name = "CURRENT_HOME_TEL")
    private String currentHomeTel;

    @Column(name = "CURRENT_MOBILE")
    @NotBlank(message = "Mobile Number is mandatory")
    private String currentMobile;

    @Column(name = "PERSONAL_EMAIL")
    private String personalEmail;

    @Column(name = "PERMANENT_ADDRESS", length = 4000)
    @NotBlank(message = "Permanent Address is mandatory")
    private String permanentAddress;

    @Column(name = "POSTAL_ADDRESS", length = 4000)
    private String postalAddress;

    @Column(name = "PASSPORT_NO")
    private String passportNo;

    @Column(name = "PASSPORT_EXPIRY_DT")
    private LocalDate passportExpiryDt;

    @Column(name = "CPR_NO")
    private String cprNo;

    @Column(name = "CPR_EXPIRY_DT")
    private LocalDate cprExpiryDt;

    @Column(name = "REMARKS", length = 4000)
    private String remarks;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DELETED")
    private String deleted = "N";

    @Column(name = "CREATED_BY")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastmodifiedBy;

    @UpdateTimestamp
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastmodifiedDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID")
    private List<HrPersonalDataDependent> dependents;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID")
    private List<HrPersonalDataEmergency> emergencyContacts;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID")
    private List<HrPersonalDataNominee> nominees;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID")
    private List<HrPersonalDataPolicies> policies;
}