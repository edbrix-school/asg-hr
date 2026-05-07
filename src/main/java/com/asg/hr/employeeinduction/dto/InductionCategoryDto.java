package com.asg.hr.employeeinduction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InductionCategoryDto {
    
    private Long inductionCatgPoid;
    private String status;
    private String description;
    private Integer seqNo;
}