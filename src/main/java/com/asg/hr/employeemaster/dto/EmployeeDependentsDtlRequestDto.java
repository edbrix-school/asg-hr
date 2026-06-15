package com.asg.hr.employeemaster.dto;

import com.asg.hr.employeemaster.enums.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDependentsDtlRequestDto {

    private ActionType actionType;
    private Long employeePoid;

    // Required for isUpdated/isDeleted. If null for isCreated, backend will generate it.
    private Long detRowId;

    @NotBlank(message = "Name Is Required")
    @Size(max = 100, message = "Name Must Be At Most 100 Characters")
    private String name;

    private LocalDate dateOfBirth;

    @NotBlank(message = "Relation Is Required")
    @Size(max = 20, message = "Relation Must Be At Most 20 Characters")
    private String relation;

    @NotBlank(message = "Gender Is Required")
    @Size(max = 20, message = "Gender Must Be At Most 20 Characters")
    private String gender;

    @Size(max = 20, message = "Nationality Must Be At Most 20 Characters")
    private String nationality;

    @Size(max = 20, message = "Passport No Must Be At Most 20 Characters")
    private String passportNo;
    private LocalDate ppExpiryDate;

    @Size(max = 20, message = "Cpr No Must Be At Most 20 Characters")
    private String cprNo;
    private LocalDate cprExpiry;

    @Size(max = 200, message = "Insu Details Must Be At Most 200 Characters")
    private String insuDetails;
    private LocalDate insuStartDt;

    @Size(max = 200, message = "Sponsor Must Be At Most 200 Characters")
    private String sponsor;
    private LocalDate rpExpiry;
}

