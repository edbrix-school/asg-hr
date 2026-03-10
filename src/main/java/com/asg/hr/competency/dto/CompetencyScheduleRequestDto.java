package com.asg.hr.competency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyScheduleRequestDto {
    
    @NotBlank(message = "Schedule description is required")
    private String scheduleDescription;
    
    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;
    
    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;
    
    private Integer seqNo;
    
    @NotNull(message = "Active status is required")
    private String active;
    
    @NotNull(message = "Evaluation date is required")
    private LocalDate evaluationDate;
    
    private Boolean recreate;
}
