package com.asg.hr.employeeinduction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HrEmployeeInductionDtlId implements Serializable {

    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    // Backward compatibility methods
    public Long getHdrPoid() {
        return transactionPoid;
    }

    public void setHdrPoid(Long hdrPoid) {
        this.transactionPoid = hdrPoid;
    }

    public Integer getSn() {
        return detRowId != null ? detRowId.intValue() : null;
    }

    public void setSn(Integer sn) {
        this.detRowId = sn != null ? sn.longValue() : null;
    }
}