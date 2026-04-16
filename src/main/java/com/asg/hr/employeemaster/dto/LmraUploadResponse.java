package com.asg.hr.employeemaster.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmraUploadResponse {
    private String status;
    private List<EmployeeDepndtsLmraDtlsResponseDto> lmraDetails;
}
