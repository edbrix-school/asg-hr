package com.asg.hr.nationality.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "HR_NATIONALITY_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "HR_NATIONALITY_MASTER_UK1", columnNames = "NATIONALITY_CODE"),
                @UniqueConstraint(name = "HR_NATIONALITY_MASTER_UK2", columnNames = "NATIONALITY_DESCRIPTION")
        }
)
public class HrNationalityMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NATION_POID", nullable = false)
    private Long nationPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "NATIONALITY_CODE", nullable = false, length = 20)
    private String nationalityCode;

    @Column(name = "NATIONALITY_DESCRIPTION", length = 100)
    private String nationalityDescription;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO", precision = 5, scale = 0)
    private Integer seqno;

    @Column(name = "DELETED", length = 1, columnDefinition = "VARCHAR2(1) DEFAULT 'N'")
    private String deleted = "N";

    @Column(name = "TICKET_AMOUNT_NORMAL", columnDefinition = "NUMBER DEFAULT 0")
    private BigDecimal ticketAmountNormal = BigDecimal.ZERO;

    @Column(name = "TICKET_AMOUNT_BUSINESS", columnDefinition = "NUMBER DEFAULT 0")
    private BigDecimal ticketAmountBusiness = BigDecimal.ZERO;

}