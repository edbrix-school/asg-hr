package com.asg.hr.airsector.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "HR_AIRSECTOR_MASTER")
public class HrAirsectorMaster extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AIRSEC_POID")
    private Long airsecPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "AIRSECTOR_CODE", unique = true)
    private String airsectorCode;

    @Column(name = "AIRSECTOR_DESCRIPTION", unique = true)
    private String airsectorDescription;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "AVERAGE_TICKET_RATE")
    private BigDecimal averageTicketRate;

    @Column(name = "HR_COUNTRY_POID")
    private Long hrCountryPoid;

    @Column(name = "BUSINESS_FARE")
    private String businessFare;

}
