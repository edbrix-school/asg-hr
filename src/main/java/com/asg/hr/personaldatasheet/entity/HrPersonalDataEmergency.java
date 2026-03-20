package com.asg.hr.personaldatasheet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "HR_PERSONAL_DATA_EMERGENCY")
@IdClass(HrPersonalDataEmergencyId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonalDataEmergency {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "RELATION")
    private String relation;

    @Column(name = "MOBILE")
    private String mobile;

    @Column(name = "HOME_TEL")
    private String homeTel;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "COUNTRY")
    private String country;

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