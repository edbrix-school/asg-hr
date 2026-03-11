package com.asg.hr.competency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyMasterRequestDto {

    @NotBlank(message = "Competency code is required")
    private String competencyCode;

    private String competencyDescription;

    private String competencyNarration;

    @Pattern(regexp = "^[YN]$", message = "Active must be either 'Y' or 'N'")
    private String active;

    private Integer seqNo;
}
