package com.asg.hr.employeemaster.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePhotoUpdateRequestDto {

    @NotNull
    private byte[] photo;
}

