package com.asg.hr.holidaymaster.dto;

import jakarta.validation.constraints.FutureOrPresent;
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
public class HolidayMasterRequest {

    private Long holidayPoid; // for update

    @NotNull(message = "Holiday date is mandatory")
    @FutureOrPresent(message = "Holiday date cannot be in the past")
    private LocalDate holidayDate;

    @NotBlank(message = "Holiday reason is mandatory")
    @Size(max = 100, message = "Holiday reason must not exceed 100 characters")
    private String holidayReason;

    private Integer seqNo;

    /**
     * Active flag; accepts Y/N or boolean-like values; normalized in mapper.
     */
    @Size(max = 1, message = "Active flag must be Y or N")
    private String active;
}

