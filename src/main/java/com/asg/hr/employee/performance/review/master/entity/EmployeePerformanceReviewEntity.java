package com.asg.hr.employee.performance.review.master.entity;

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
public class EmployeePerformanceReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(generator = "trigger-generated")
    @org.hibernate.annotations.GenericGenerator(
        name = "trigger-generated",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "HR_COMPETENCY_MASTER_SEQ"),
            @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
        }
    )
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
