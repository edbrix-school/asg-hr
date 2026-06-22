package com.asg.hr.competency.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HR_COMPETENCY_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPETENCY_POID")
    private Long competencyPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPETENCY_CODE", unique = true, nullable = false)
    private String competencyCode;

    @Column(name = "COMPETENCY_DESCRIPTION")
    private String competencyDescription;

    @Column(name = "COMPETENCY_NARRATION")
    private String competencyNarration;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "DELETED")
    private String deleted;
}
