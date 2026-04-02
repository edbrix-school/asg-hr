package com.asg.hr.designation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class DesignationRequest {

    @NotBlank(message = "Designation code is mandatory")
    @Size(max = 20, message = "Designation code cannot exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Designation code must be alphanumeric")
    private String designationCode;

    @NotBlank(message = "Designation name is mandatory")
    @Size(max = 100, message = "Designation name cannot exceed 100 characters")
    private String designationName;

    @NotBlank(message = "Job description is mandatory")
    private String jobDescription;

    private String skillDescription;

    private Long reportingToPoid;

    private Long seqNo;

    @Pattern(regexp = "[YN]", message = "Active flag must be either Y or N")
    @Size(max = 1, message = "Active flag must be a single character")
    private String active;
}

