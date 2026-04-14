package com.asg.hr.holidaymaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HolidayMasterResponse {

    private Long holidayPoid;

    private LocalDate holidayDate;

    private String holidayReason;

    private String status;

    private String active;

    private String deleted;

    private BigInteger seqNo;

    private LocalDateTime createdDate;

    private String createdBy;

    private LocalDateTime modifiedDate;

    private String modifiedBy;
}

