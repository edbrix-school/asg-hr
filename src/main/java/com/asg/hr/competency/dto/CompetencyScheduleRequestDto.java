package com.asg.hr.competency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Active cannot be blank")
    @Pattern(regexp = "^[YN]$", message = "must be either 'Y' or 'N'")
    @Size(max = 1, message = "Max character for active is 1 - 'Y' or 'N'")
    private String active;
    
    @NotNull(message = "Evaluation date is required")
    private LocalDate evaluationDate;
    
    private Boolean recreate;
}
