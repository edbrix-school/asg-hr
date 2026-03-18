package com.asg.hr.location.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GLOBAL_LOCATION_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOCATION_POID")
    private Long locationPoid;

    @Column(name = "COMPANY_POID")
    private Long company;

    @Column(name = "LOCATION_CODE", nullable = false)
    private String locationCode;

    @Column(name = "LOCATION_NAME", nullable = false)
    private String locationName;

    @Column(name = "LOCATION_NAME2")
    private String locationName2;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "SITE_SUPERVISOR_USER_POID")
    private Long siteSupervisorUserPoid;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "DELETED")
    private String deleted;
}