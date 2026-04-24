package com.asg.hr.competency.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}
