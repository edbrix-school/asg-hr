package com.asg.hr.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADMIN_CR_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCrMaster {

    @Id
    @Column(name = "CR_POID")
    private Long crPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "CR_NUMBER", length = 20, nullable = false, unique = true)
    private String crNumber;

    @Column(name = "CR_NAME", length = 100, nullable = false, unique = true)
    private String crName;

    @Column(name = "NO_OF_VISA_APPROVED")
    private Long noOfVisaApproved;

    @Column(name = "NATIONALIZATION_PERCENT", length = 20)
    private String nationalizationPercent;

    @Column(name = "NO_OF_VISA_USED")
    private Long noOfVisaUsed;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "CR_EXPIRY_DATE")
    private LocalDate crExpiryDate;

    @Column(name = "CR_REGISTRATION_DATE")
    private LocalDate crRegistrationDate;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "PMA_LICENCE_EXPIRY")
    private LocalDate pmaLicenceExpiry;

    @Column(name = "CHAMBER_OF_COMMERCE_EXPIRY")
    private LocalDate chamberOfCommerceExpiry;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "CR_SHORT_NAME", length = 30)
    private String crShortName;

    @Column(name = "COMPANY_DIV_POID")
    private Long companyDivPoid;

}