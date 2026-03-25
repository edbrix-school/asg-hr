package com.asg.hr.holidaymaster.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HolidayBatchCreateRequest {

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    @NotBlank(message = "Holiday reason is mandatory")
    @Size(max = 100, message = "Holiday reason must not exceed 100 characters")
    private String reason;

    @NotNull(message = "Number of days is mandatory")
    @Min(value = 1, message = "Days must be at least 1")
    private Integer days;
}

