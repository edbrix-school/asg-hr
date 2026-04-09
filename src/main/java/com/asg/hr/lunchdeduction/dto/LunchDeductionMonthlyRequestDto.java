package com.asg.hr.lunchdeduction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchDeductionMonthlyRequestDto {

    private LocalDate transactionDate;

    @NotNull
    private LocalDate payrollMonth;

    @Size(max = 100)
    private String lunchDescription;

    @Size(max = 500)
    private String remarks;

    @Valid
    @Builder.Default
    private List<LunchDeductionDetailRequestDto> details = new ArrayList<>();
}