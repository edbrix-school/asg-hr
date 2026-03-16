package com.asg.hr.airsector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HrAirsectorRequestDto {

    private Long groupPoid;
    @NotBlank(message = "Airsector description is mandatory")
    private String airsectorDescription;
    private String active;
    private Integer seqno;
    private BigDecimal averageTicketRate;
    @NotNull(message = "Country POID is mandatory")
    private Long hrCountryPoid;
    private String businessFare;
}
