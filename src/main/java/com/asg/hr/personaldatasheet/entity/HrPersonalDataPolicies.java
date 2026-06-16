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
@Table(name = "HR_PERSONAL_DATA_POLICIES")
@IdClass(HrPersonalDataPoliciesId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonalDataPolicies {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "DOC_POID")
    private Long docPoid;

    @Column(name = "DOC_NAME")
    private String docName;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 4000)
    private String drilldownLinkInfo;

    @Column(name = "POLICY_ACCEPTED")
    private String policyAccepted;

    @Column(name = "POLICY_ACCEPTED_ON")
    private LocalDate policyAcceptedOn;

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