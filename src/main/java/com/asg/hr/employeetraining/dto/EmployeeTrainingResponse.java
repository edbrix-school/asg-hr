package com.asg.hr.employeetraining.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTrainingResponse {

    private Long transactionPoid;
    private String docRef;
    private String employeePoid;
    private LovGetListDto employeeDet;
    private String courseName;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private Integer durationDays;
    private String trainingType;
    private LovGetListDto trainingTypeDet;
    private String institution;
    private BigDecimal trainingCost;
    private String trainingLocation;
    private String remarks;
    private LocalDate transactionDate;
    private Long companyPoid;
    private Long groupPoid;
    private String deleted;
    private String active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private List<EmployeeTrainingDetailResponse> details = new ArrayList<>();
}
