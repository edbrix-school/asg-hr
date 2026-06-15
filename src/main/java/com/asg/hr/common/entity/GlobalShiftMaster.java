package com.asg.hr.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GLOBAL_SHIFT_MASTER")
public class GlobalShiftMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shift_seq_gen")
    @SequenceGenerator(
            name = "shift_seq_gen",
            sequenceName = "GLOBAL_SHIFT_MASTER_SEQ",
            allocationSize = 1
    )
    @Column(name = "SHIFT_POID")
    private Long shiftPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "SHIFT_CODE", length = 20)
    private String shiftCode;

    @Column(name = "SHIFT_LOCATION", length = 20)
    private String shiftLocation;

    @Column(name = "SHIFT_NAME", length = 20)
    private String shiftName;

    @Column(name = "SHIFT_START")
    private LocalDateTime shiftStart;

    @Column(name = "SHIFT_END")
    private LocalDateTime shiftEnd;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "SHIFT_HRS")
    private Double shiftHrs;

    @Column(name = "LUNCH_HRS")
    private Long lunchHrs;

    @Column(name = "SAT_HRS")
    private Double satHrs;
}