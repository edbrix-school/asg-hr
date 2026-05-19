package com.asg.hr.employeeinduction.util;

import com.asg.common.lib.security.util.UserContext;
import com.asg.hr.employeeinduction.dto.EmployeeInductionRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionResponseDto;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtl;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionHdr;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmployeeInductionMapper {

    public HrEmployeeInductionHdr toEntity(EmployeeInductionRequestDto dto) {
        return HrEmployeeInductionHdr.builder()
                .docRef(dto.getDocId())
                .employeePoid(dto.getEmployeePoid())
                .remarks(dto.getRemarks())
                .companyPoid(UserContext.getCompanyPoid()) // Default company
                .transactionDate(LocalDate.now())
                .build();
    }

    public EmployeeInductionResponseDto toResponseDto(HrEmployeeInductionHdr entity) {
        List<EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto> detailDtos = 
                entity.getDetails() != null ? 
                entity.getDetails().stream()
                        .filter(detail -> !EmployeeInductionConstants.DELETED_YES.equals(detail.getDeleted()))
                        .map(this::toDetailResponseDto)
                        .collect(Collectors.toList()) : null;

        return EmployeeInductionResponseDto.builder()
                .poid(entity.getPoid())
                .docId(entity.getDocId())
                .employeePoid(entity.getEmployeePoid())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .remarks(entity.getRemarks())
                .details(detailDtos)
                .build();
    }

    public HrEmployeeInductionDtl toDetailEntity(HrEmployeeInductionHdr header, 
                                               EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto dto) {
        return HrEmployeeInductionDtl.builder()
                .transactionPoid(header.getTransactionPoid())
                .detRowId(dto.getSn().longValue())
                .header(header)
                .inductionCatgPoid(dto.getInductionCategory() != null ? Long.parseLong(dto.getInductionCategory()) : null)
                .sheduledDate(dto.getScheduledDate())
                .compleatedDate(dto.getCompletedDate())
                .status(dto.getStatus())
                .remarks(dto.getRemarks())
                .build();
    }

    public EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto toDetailResponseDto(HrEmployeeInductionDtl entity) {
        return EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto.builder()
                .sn(entity.getDetRowId() != null ? entity.getDetRowId().intValue() : null)
                .inductionCategory(entity.getInductionCatgPoid() != null ? entity.getInductionCatgPoid().toString() : null)
                .assigneePoid(null) // Column doesn't exist in actual table
                .scheduledDate(entity.getSheduledDate())
                .completedDate(entity.getCompleatedDate())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .build();
    }
}
