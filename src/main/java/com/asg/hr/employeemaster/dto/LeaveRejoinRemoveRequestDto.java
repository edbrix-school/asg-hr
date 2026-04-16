package com.asg.hr.employeemaster.dto;

import jakarta.validation.constraints.NotBlank;
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
public class LeaveRejoinRemoveRequestDto {

    @NotBlank(message = "Leave Request Reference Is Required")
    private String rejoinLrqRef;

    private LocalDate rejoinDate;
}
