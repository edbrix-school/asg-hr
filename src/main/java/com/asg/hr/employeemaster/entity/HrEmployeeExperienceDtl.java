package com.asg.hr.employeemaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(
        name = "HR_EMPLOYEE_EXPERIENCE_DTL",
        uniqueConstraints = {
                @UniqueConstraint(name = "HR_EMPLOYEE_EXPERIENCE_UK1", columnNames = "EMPLOYER")
        }
)
@IdClass(HrEmployeeExperienceDtlId.class)
public class HrEmployeeExperienceDtl extends BaseEntity {

    @Id
    @Column(name = "EMPLOYEE_POID", nullable = false)
    private Long employeePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "EMPLOYER", length = 200, unique = true)
    private String employer;

    @Column(name = "COUNTRY_LOCATION", length = 100)
    private String countryLocation;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "TO_DATE")
    private LocalDate toDate;

    @Column(name = "MONTHS", length = 10)
    private String months;

    @Column(name = "DESIGNATION", length = 200)
    private String designation;
}