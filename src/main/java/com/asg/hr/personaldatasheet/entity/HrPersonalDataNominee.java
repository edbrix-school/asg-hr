package com.asg.hr.personaldatasheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "HR_PERSONAL_DATA_NOMINEE")
@IdClass(HrPersonalDataNomineeId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrPersonalDataNominee {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "NOMINEE_TYPE")
    private String nomineeType;

    @Column(name = "PERCENTAGE")
    @NotNull(message = "Percentage is mandatory")
    private BigDecimal percentage;

    @Column(name = "NOMINEE_NAME")
    @NotBlank(message = "Primary Nominee is mandatory")
    private String nomineeName;

    @Column(name = "RELATION")
    private String relation;

    @Column(name = "MOBILE")
    private String mobile;

    @Column(name = "ADDRESS", length = 4000)
    private String address;

    @Column(name = "BANK_DETIALS", length = 4000)
    private String bankDetails;

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