package com.asg.hr.leaverequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class HrLeaveRequestDtlId  implements Serializable {

    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;
}
