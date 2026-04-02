package com.asg.hr.designation.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationResponse {

    private Long designationPoid;
    private Long groupPoid;
    private String designationCode;
    private String designationName;
    private String jobDescription;
    private String skillDescription;
    private Long reportingToPoid;
    private Long seqNo;
    private String active;
    private String deleted;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
    private String lastModifiedBy;
}

