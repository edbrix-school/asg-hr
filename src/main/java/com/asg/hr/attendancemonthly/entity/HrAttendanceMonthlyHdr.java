package com.asg.hr.attendancemonthly.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "HR_ATTENDANCE_MONTHLY_HDR", uniqueConstraints = {
        @UniqueConstraint(name = "UK_DOCREFFHR_MONTHLY_HDR", columnNames = {"DOC_REF"})
})
public class HrAttendanceMonthlyHdr extends BaseEntity {

    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @AuditIgnore
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID", length = 20)
    private String companyPoid;

    @Column(name = "ATTENDANCE_FROM")
    private LocalDate attendanceFrom;

    @Column(name = "ATTENDANCE_TO")
    private LocalDate attendanceTo;

    @Column(name = "ATTENDANCE_DESCRIPTION", length = 100)
    private String attendanceDescription;

    @Column(name = "EMPLOYEE_WISE", length = 1)
    private String employeeWise;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "DOC_REF", length = 25, unique = true, insertable = false, updatable = false)
    @Generated
    private String docRef;

    @AuditIgnore
    @Column(name = "DELETED", length = 1)
    private String deleted = "N";

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "LOADED_PAYROLL", length = 1)
    private String loadedPayroll = "N";
}
