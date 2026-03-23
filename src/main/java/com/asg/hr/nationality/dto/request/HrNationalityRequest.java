package com.asg.hr.nationality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrNationalityRequest {

    @NotBlank(message = "Nationality Code is required")
    private String nationalityCode;

    private String nationalityDescription;

    @NotNull(message = "Active status is required")
    private Boolean active;

    private Integer seqNo;

    private BigDecimal ticketAmountNormal;

    private BigDecimal ticketAmountBusiness;
}
