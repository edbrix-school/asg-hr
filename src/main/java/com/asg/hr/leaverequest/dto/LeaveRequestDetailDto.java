package com.asg.hr.leaverequest.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestDetailDto {

    private Long detRowId;
    private String name;
    private String relation;
    private String ticketAgeGroup;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String remarks;

    /**
     * ISCREATED  - new row to insert
     * ISUPDATED  - existing row to update
     * ISDELETED  - existing row to delete
     * NOCHANGE   - no action needed
     */
    private String actionType;
}
