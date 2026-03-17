package com.asg.hr.allowanceanddeductionmaster.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceDeductionMasterRequestDTO {

    @NotBlank(message = "Code is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Code must be alphanumeric")
    @Size(max = 20, message = "Code must be at most 20 characters")
    private String code;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    @NotBlank(message = "Variable Fixed is mandatory")
    @Size(max = 20, message = "Variable Fixed must be at most 20 characters")
    private String variableFixed;

    @NotBlank(message = "Type is mandatory")
    @Pattern(regexp = "ALLOWANCE|DEDUCTION", message = "Type must be either ALLOWANCE or DEDUCTION")
    private String type;

    @Size(max = 500, message = "Formula must be at most 500 characters")
    private String formula;

    @NotNull(message = "GL is mandatory")
    private Long glPoid;

    @Pattern(regexp = "Y|N", message = "Mandatory must be Y or N")
    private String mandatory;

    @Size(max = 30, message = "Payroll Field Name must be at most 30 characters")
    private String payrollFieldName;

    @Min(value = 0, message = "Sequence number cannot be negative")
    @Max(value = 99999, message = "Sequence number cannot exceed 5 digits")
    private Integer seqno;

    @Pattern(regexp = "Y|N", message = "Active must be Y or N")
    private String active;

    private Long groupPoid;
}
