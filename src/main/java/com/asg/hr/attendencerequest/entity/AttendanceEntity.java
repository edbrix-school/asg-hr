package com.asg.hr.attendencerequest.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "HR_ATTENDANCE_SPECIAL_REQ")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "att_seq")
    @SequenceGenerator(name = "att_seq", sequenceName = "HR_ATTENDANCE_SPECIAL_REQ_SEQ", allocationSize = 1)
    @Column(name = "TRANSACTION_POID")
    @AuditIgnore
    private Long attendancePoid;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25)
    @AuditIgnore
    private String docRef;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "ATTENDANCE_DATE")
    private LocalDate attendanceDate;

    @Column(name = "EXCEPTION_TYPE", length = 20)
    private String exceptionType;

    @Column(name = "EXCEPTION_REASON", length = 200)
    private String reason;

    @Column(name = "HOD_REMARKS", length = 500)
    private String hodRemarks;

    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "OT1_HOURS", length = 10)
    private String ot1Hours;

    @Column(name = "OT2_HOURS", length = 10)
    private String ot2Hours;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;

}