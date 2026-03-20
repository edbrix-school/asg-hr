package com.asg.hr.personaldatasheet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "HR_PERSONAL_DATA_DEPENDENT")
@IdClass(HrPersonalDataDependentId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonalDataDependent {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "NAME_PASSPORT")
    private String namePassport;

    @Column(name = "RELATION")
    private String relation;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "NATIONALITY")
    private String nationality;

    @Column(name = "PASSPORT_NO")
    private String passportNo;

    @Column(name = "PP_EXPIRY_DATE")
    private LocalDate ppExpiryDate;

    @Column(name = "CPR_NO")
    private String cprNo;

    @Column(name = "CPR_EXPIRY")
    private LocalDate cprExpiry;

    @Column(name = "RP_EXPIRY")
    private LocalDate rpExpiry;

    @Column(name = "VISA_SPONSOR")
    private String visaSponsor;

    @Column(name = "MOBILE")
    private String mobile;

    @Column(name = "TELPHONE")
    private String telephone;

    @Column(name = "REMARKS", length = 4000)
    private String remarks;

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
}