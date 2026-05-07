package com.asg.hr.leaverejoin.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "HR_EMP_REJOIN_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrEmployeeRejoinHdr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "LEAVE_REQUEST_POID")
    private Long leaveRequestPoid;

    @Column(name = "DATE_OF_REJOINING")
    private LocalDate dateOfRejoining;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "PASSPORT_RECEIVED", length = 4)
    private String passportReceived;

    @Column(name = "RECEIVED_BY", length = 100)
    private String receivedBy;

    @Column(name = "REMARKS_BY_HR", length = 500)
    private String remarksByHr;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "EXTRA_LEAVE_DAYS")
    private Integer extraLeaveDays;

    @Column(name = "EXTRA_ABSENT_DAYS")
    private Integer extraAbsentDays;
}
