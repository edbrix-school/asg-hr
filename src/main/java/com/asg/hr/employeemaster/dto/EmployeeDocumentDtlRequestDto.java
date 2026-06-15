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
public class EmployeeDocumentDtlRequestDto {

    private ActionType actionType;
    private Long employeePoid;
    private Long detRowId;

    @NotBlank(message = "Doc Name Is Required")
    @Size(max = 100, message = "Doc Name Must Be At Most 100 Characters")
    private String docName;
    private LocalDate expiryDate;

    @Size(max = 150, message = "Remarks Must Be At Most 150 Characters")
    private String remarks;
}

