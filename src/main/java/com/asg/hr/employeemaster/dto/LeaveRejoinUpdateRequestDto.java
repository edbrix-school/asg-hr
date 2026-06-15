package com.asg.hr.employeemaster.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRejoinUpdateRequestDto {

    @NotNull(message = "Rejoin Date Is Required")
    private LocalDate rejoinDate;

    private String rejoinLrqRef;
}
