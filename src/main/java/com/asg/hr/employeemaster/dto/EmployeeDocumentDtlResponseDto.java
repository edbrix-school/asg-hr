package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDocumentDtlResponseDto {

    private Long employeePoid;
    private Long detRowId;
    private String docName;
    private LocalDate expiryDate;
    private String remarks;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

