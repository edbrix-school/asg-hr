package com.asg.hr.nationality.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HrNationalityUpdateRequest {

    @JsonAlias("NATIONALITY_CODE")
    private String nationalityCode;

    @JsonAlias("NATIONALITY_DESCRIPTION")
    private String nationalityDescription;

    @NotNull(message = "Active status is required")
    @JsonAlias("ACTIVE")
    private Boolean active;

    @JsonAlias("SEQNO")
    private Integer seqNo;

    @JsonAlias("TICKET_AMOUNT_NORMAL")
    private BigDecimal ticketAmountNormal;

    @JsonAlias("TICKET_AMOUNT_BUSINESS")
    private BigDecimal ticketAmountBusiness;
}
