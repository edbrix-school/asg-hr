package com.asg.hr.locationmaster.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationMasterResponseDto {

    private Long locationPoid;
    private Long companyPoid;
    private String locationCode;
    private String locationName;
    private String locationName2;
    private String address;
    private Long siteSupervisorUserPoid;
    private LovGetListDto siteSupervisorDet;
    private String active;
    private Integer seqno;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}