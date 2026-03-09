package com.asg.hr.competency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyScheduleListDto {
    
    private Long schedulePoid;
    private String scheduleDescription;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String active;
}
