package com.asg.hr.employeeinduction.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "HR_EMPLOYEE_INDUCTION_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(HrEmployeeInductionDtlId.class)
public class HrEmployeeInductionDtl extends BaseEntity {

    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private HrEmployeeInductionHdr header;

    @Column(name = "INDUCTION_CATG_POID")
    private Long inductionCatgPoid;

    @Column(name = "SHEDULED_DATE")
    private LocalDate sheduledDate;

    @Column(name = "COMPLEATED_DATE")
    private LocalDate compleatedDate;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "N";
        }
    }

    // Backward compatibility methods
    public HrEmployeeInductionDtlId getId() {
        return new HrEmployeeInductionDtlId(transactionPoid, detRowId.longValue());
    }

    public void setId(HrEmployeeInductionDtlId id) {
        if (id != null) {
            this.transactionPoid = id.getHdrPoid();
            this.detRowId = id.getSn().longValue();
        }
    }

    // Compatibility getters/setters
    public Long getHdrPoid() {
        return transactionPoid;
    }

    public void setHdrPoid(Long hdrPoid) {
        this.transactionPoid = hdrPoid;
    }

    public Integer getSequenceNo() {
        return detRowId != null ? detRowId.intValue() : null;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.detRowId = sequenceNo != null ? sequenceNo.longValue() : null;
    }

    public String getInductionCategory() {
        return inductionCatgPoid != null ? inductionCatgPoid.toString() : null;
    }

    public void setInductionCategory(String inductionCategory) {
        try {
            this.inductionCatgPoid = inductionCategory != null ? Long.parseLong(inductionCategory) : null;
        } catch (NumberFormatException e) {
            this.inductionCatgPoid = null;
        }
    }

    public Long getAssigneePoid() {
        return null; // Column doesn't exist in actual table
    }

    public void setAssigneePoid(Long assigneePoid) {
        // No-op since column doesn't exist
    }

    public LocalDate getScheduledDate() {
        return sheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.sheduledDate = scheduledDate;
    }

    public LocalDate getCompletedDate() {
        return compleatedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.compleatedDate = completedDate;
    }

    public String getDeleted() {
        return "N"; // Default value since column doesn't exist
    }

    public void setDeleted(String deleted) {
        // No-op since column doesn't exist
    }
}