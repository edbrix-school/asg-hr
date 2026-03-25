package com.asg.hr.religion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReligionDtoRequest {

    @NotBlank(message = "Religion Code is required")
    @Size(max = 20, message = "Religion Code must be less than or equal to 20 characters")
    private String religionCode;

    @Size(max = 100, message = "Religion description must be less than or equal to 100 characters")
    private String description;

    @NotBlank(message = "Active status is required")
    private String active;

    private Long seqNo;

}
