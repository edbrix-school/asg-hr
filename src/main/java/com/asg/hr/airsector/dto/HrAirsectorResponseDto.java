package com.asg.hr.airsector.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class HrAirsectorResponseDto {

    private Long airsecPoid;
    private Long groupPoid;
    private String airsectorCode;
    private String airsectorDescription;
    private String active;
    private Integer seqno;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastmodifiedBy;
    private LocalDateTime lastmodifiedDate;
    private String deleted;
    private BigDecimal averageTicketRate;
    private Long hrCountryPoid;
    private LovGetListDto countryDtl;
    private String businessFare;
}
