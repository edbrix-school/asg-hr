package com.asg.hr.nationality.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HrNationalityResponse {

    private Long nationPoid;
    private String nationalityCode;
    private String nationalityDescription;
    private Boolean active;
    private Integer seqNo;
    private BigDecimal ticketAmountNormal;
    private BigDecimal ticketAmountBusiness;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
