package com.asg.hr.locationmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationMasterRequestDto {

    @NotBlank(message = "Location code is required")
    @Size(max = 20, message = "Location code must not exceed 20 characters")
    private String locationCode;

    @NotBlank(message = "Location name is required")
    @Size(max = 100, message = "Location name must not exceed 100 characters")
    private String locationName;

    @Size(max = 100, message = "Location name 2 must not exceed 100 characters")
    private String locationName2;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Long siteSupervisorUserPoid;

    private String active;

    private Integer seqno;
}