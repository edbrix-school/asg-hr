package com.asg.hr.leaverejoin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class   EmployeeLeaveRejoinRequest {

    private LocalDate transactionDate;

    @NotNull(message = "Employee Poid is mandatory")
    @Positive(message = "Employee Poid must be positive")
    private Long employeePoid;

    @NotNull(message = "Leave Request Poid is mandatory")
    @Positive(message = "Leave Request Poid must be positive")
    private Long leaveRequestPoid;

    @NotNull(message = "Date of rejoining is mandatory")
    private LocalDate dateOfRejoining;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @Size(max = 4, message = "Passport received must not exceed 4 characters")
    private String passportReceived;

    @Size(max = 100, message = "Received by must not exceed 100 characters")
    private String receivedBy;

    @Size(max = 500, message = "Remarks by HR must not exceed 500 characters")
    private String remarksByHr;

    private Integer extraLeaveDays;

    private Integer extraAbsentDays;
}
