package com.asg.hr.employeemaster.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePhotoUpdateResponseDto {
    private Long employeePoid;
    private byte[] photo;
}

