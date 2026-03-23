package com.asg.hr.airsector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HrAirsectorRequestDto {

    private Long groupPoid;
    @NotBlank(message = "Airsector description is mandatory")
    private String airsectorDescription;
    @Pattern(regexp = "^[YN]$", message = "Active must be either 'Y' or 'N'")
    @Size(max = 1, message = "Max character for active is 1")
    private String active;
    private Integer seqno;
    private BigDecimal averageTicketRate;
    @NotNull(message = "Country POID is mandatory")
    private Long hrCountryPoid;
    private String businessFare;
}
