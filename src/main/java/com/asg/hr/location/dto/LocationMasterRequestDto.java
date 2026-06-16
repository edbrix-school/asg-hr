package com.asg.hr.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationMasterRequestDto {

    @NotBlank(message = "Location code is required")
    private String locationCode;

    @NotBlank(message = "Location name is required")
    private String locationName;

    private String locationName2;

    private String address;

    private Long siteSupervisorUserPoid;

    @Pattern(regexp = "^[YN]$", message = "Active must be either 'Y' or 'N'")
    @Size(max = 1, message = "Max character for active is 1")
    private String active;
}