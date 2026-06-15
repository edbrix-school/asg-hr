package com.asg.hr.employeemaster.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "HR_EMPLOYEE_DOCUMENT_DTL")
@IdClass(HrEmployeeDocumentDtlId.class)
public class HrEmployeeDocumentDtl extends BaseEntity {

    @Id
    @Column(name = "EMPLOYEE_POID", nullable = false)
    private Long employeePoid;

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Column(name = "DOC_NAME", length = 100)
    private String docName;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "REMARKS", length = 150)
    private String remarks;
}