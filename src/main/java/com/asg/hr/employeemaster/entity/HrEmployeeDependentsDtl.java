package com.asg.hr.employeemaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "HR_EMPLOYEE_DEPENDENTS_DTL",
        uniqueConstraints = {
                @UniqueConstraint(name = "HR_EMPLOYEE_DEPENDENTS_UK1", columnNames = "NAME")
        }
)
@IdClass(HrEmployeeDependentsDtlId.class)
public class HrEmployeeDependentsDtl extends BaseEntity {

    @Id
    @Column(name = "EMPLOYEE_POID", nullable = false)
    private Long employeePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "NAME", length = 100, unique = true)
    private String name;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    @Column(name = "RELATION", length = 20)
    private String relation;

    @Column(name = "GENDER", length = 20)
    private String gender;

    @Column(name = "NATIONALITY", length = 20)
    private String nationality;

    @Column(name = "PASSPORT_NO", length = 20)
    private String passportNo;

    @Column(name = "PP_EXPIRY_DATE")
    private LocalDate ppExpiryDate;

    @Column(name = "CPR_NO", length = 20)
    private String cprNo;

    @Column(name = "CPR_EXPIRY")
    private LocalDate cprExpiry;

    @Column(name = "INSU_DETAILS", length = 200)
    private String insuDetails;

    @Column(name = "INSU_START_DT")
    private LocalDate insuStartDt;

    @Column(name = "SPONSOR", length = 200)
    private String sponsor;

    @Column(name = "RP_EXPIRY")
    private LocalDate rpExpiry;
}