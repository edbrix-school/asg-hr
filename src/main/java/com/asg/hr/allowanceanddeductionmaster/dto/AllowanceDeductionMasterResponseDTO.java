package com.asg.hr.allowanceanddeductionmaster.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceDeductionMasterResponseDTO {

    private Long allowaceDeductionPoid;
    private Long groupPoid;
    private String code;
    private String description;
    private String variableFixed;
    private String type;
    private String formula;
    private String glcode;
    private String mandatory;
    private String active;
    private Integer seqno;
    private String deleted;
    private Long glPoid;
    private String payrollFieldName;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
