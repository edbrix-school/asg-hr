package com.asg.hr.competency.dto;

import jakarta.validation.constraints.Digits;
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
    @Size(max = 100, message = "Schedule description must not exceed 100 characters")
    private String scheduleDescription;
    
    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;
    
    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;
    
    @Digits(integer = 5, fraction = 0, message = "Seq No must be a whole number with up to 5 digits")
    private Integer seqNo;

    @NotBlank(message = "Active cannot be blank")
    @Pattern(regexp = "^[YN]$", message = "must be either 'Y' or 'N'")
    @Size(max = 1, message = "Max character for active is 1 - 'Y' or 'N'")
    private String active;
    
    @NotNull(message = "Evaluation date is required")
    private LocalDate evaluationDate;
    
    private Boolean recreate;
}
