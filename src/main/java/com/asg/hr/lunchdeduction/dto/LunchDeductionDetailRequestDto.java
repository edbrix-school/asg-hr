package com.asg.hr.lunchdeduction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchDeductionDetailRequestDto {

    @Positive
    private Long detRowId;

    @Positive
    private Long employeePoid;

    @Size(max = 30)
    private String deductionType;

    @PositiveOrZero
    private Long monthDays;

    @PositiveOrZero
    private Long offDays;

    @PositiveOrZero
    private Long lunchDays;

    @PositiveOrZero
    private BigDecimal costPerDay;

    @Size(max = 20)
    private String userId;

    @Size(max = 100)
    private String userName;

    @Size(max = 200)
    private String remarks;

    @NotNull
    private LunchDeductionActionType actionType;
}