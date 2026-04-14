package com.asg.hr.religion.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReligionDtoResponse {
    private Long religionPoid;
    private Long groupPoid;
    private String religionCode;
    private String description;
    private String active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private Long seqNo;
}
