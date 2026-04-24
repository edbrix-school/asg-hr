package com.asg.hr.competency.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "HR_COMPETENCY_SCHEDULE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrCompetencySchedule extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMP_SCHEDULE_POID")
    @AuditIgnore
    private Long schedulePoid;
    
    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;
    
    @Column(name = "SCHEDULE_DESCRIPTION", length = 100)
    private String scheduleDescription;
    
    @Column(name = "PERIOD_FROM")
    private LocalDate periodFrom;
    
    @Column(name = "PERIOD_TO")
    private LocalDate periodTo;
    
    @Column(name = "SEQNO")
    private Integer seqNo;
    
    @Column(name = "ACTIVE", length = 1)
    private String active;
    
    @Column(name = "EVALUATION_DATE")
    private LocalDate evaluationDate;
    
    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;
}
