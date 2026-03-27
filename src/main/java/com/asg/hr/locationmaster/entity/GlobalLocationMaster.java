package com.asg.hr.locationmaster.entity;

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
public class GlobalLocationMaster extends BaseEntity {

    @Id
    @Column(name = "LOCATION_POID")
    private Long locationPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    private Long companyPoid;

    @Column(name = "LOCATION_CODE", length = 20, nullable = false)
    private String locationCode;

    @Column(name = "LOCATION_NAME", length = 100, nullable = false)
    private String locationName;

    @Column(name = "LOCATION_NAME2", length = 100)
    private String locationName2;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "STAFF_SALE_DISCOUNT")
    private Double staffSaleDiscount;

    @Column(name = "STAFF_DISCOUNT")
    private Double staffDiscount;

    @Column(name = "LOYALTY_CUSTOMER_DISCOUNT")
    private Double loyaltyCustomerDiscount;

    @Column(name = "DISCOUNT_ENABLED", length = 1)
    private String discountEnabled;

    @Column(name = "SITE_SUPERVISOR_USER_POID")
    private Long siteSupervisorUserPoid;

    @Column(name = "OWN_LOCATION", length = 1)
    private String ownLocation;

    @Column(name = "INVENTORY_LOCATION", length = 1)
    private String inventoryLocation;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "IS_WAREHOUSE", length = 1)
    private String isWarehouse;


}