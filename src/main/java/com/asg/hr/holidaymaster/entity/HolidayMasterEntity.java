package com.asg.hr.holidaymaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;

@Entity
@Table(name = "HR_HOLIDAY_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HOLIDAY_POID", nullable = false)
    private Long holidayPoid;

    @Column(name = "HOLIDAY_DATE")
    private LocalDate holidayDate;

    @Column(name = "HOLIDAY_REASON", length = 100)
    private String holidayReason;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private BigInteger seqno;
}

