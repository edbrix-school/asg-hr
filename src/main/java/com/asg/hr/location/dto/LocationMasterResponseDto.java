package com.asg.hr.location.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationMasterResponseDto {

    private Long locationPoid;
    private String locationCode;
    private String locationName;
    private String locationName2;
    private String address;
    private Long siteSupervisorUserPoid;
    private String active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}