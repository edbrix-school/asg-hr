package com.asg.hr.competency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 1, message = "Max character for active is 1")
    private String active;

    private Integer seqNo;
}
