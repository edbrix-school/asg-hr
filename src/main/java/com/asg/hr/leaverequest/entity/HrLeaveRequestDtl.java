package com.asg.hr.leaverequest.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;


@Data
@Entity
@Table(name = "HR_LEAVE_REQUEST_DTL")
public class HrLeaveRequestDtl extends BaseEntity {

    @EmbeddedId
    private HrLeaveRequestDtlId id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "RELATION")
    private String relation;

    @Column(name = "DATE_FROM")
    private LocalDate dateFrom;

    @Column(name = "DATE_TO")
    private LocalDate dateTo;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "TICKET_AGE_GROUP")
    private String ticketAgeGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private HrLeaveRequestHdrEntity leaveRequestHdr;
}
