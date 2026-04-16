package com.asg.hr.allowanceanddeductionmaster.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceDeductionResponseDTO {

    private Long allowaceDeductionPoid;
    private Long groupPoid;
    private String code;
    private String description;
    private String variableFixed;
    private LovGetListDto variableFixedLov;
    private String type;
    private LovGetListDto typeLov;
    private String formula;
    private String glcode;
    private Long glPoid;
    private LovGetListDto glLov;
    private String mandatory;
    private String active;
    private Integer seqno;
    private String deleted;
    private String payrollFieldName;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
