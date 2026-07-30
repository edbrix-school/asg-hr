package com.asg.hr.competency.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyScheduleResponseDto {
    
    private Long schedulePoid;
    private String scheduleDescription;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private Integer seqNo;
    private String active;
    private LocalDate evaluationDate;
    private LovGetListDto scheduleDtl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime modifiedDate;

    private String modifiedBy;
}
