package com.asg.hr.employeetraining.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "HR_EMPLOYEE_TRAINING_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTrainingDetailEntity extends BaseEntity {

    @EmbeddedId
    private EmployeeTrainingDetailId id;

    @Column(name = "EMP_POID")
    private Long empPoid;

    @Column(name = "TRAINING_STATUS", length = 20)
    private String trainingStatus;

    @Column(name = "COMPLETED_ON")
    private LocalDate completedOn;

    @Column(name = "OTHER_REMARKS", length = 200)
    private String otherRemarks;

}
