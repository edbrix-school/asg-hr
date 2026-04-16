package com.asg.hr.allowanceanddeductionmaster.mapper;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LovDataService;
import com.asg.hr.allowanceanddeductionmaster.dto.*;
import com.asg.hr.allowanceanddeductionmaster.entity.HrAllowanceDeductionMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllowanceDeductionMasterMapper {

    private final LovDataService lovService;

    public HrAllowanceDeductionMaster toEntity(AllowanceDeductionRequestDTO request) {
        return HrAllowanceDeductionMaster.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .variableFixed(request.getVariableFixed())
                .type(request.getType())
                .formula(request.getFormula())
                .glPoid(request.getGlPoid())
                .mandatory(request.getMandatory() != null ? request.getMandatory() : "N")
                .payrollFieldName(request.getPayrollFieldName())
                .seqno(request.getSeqno())
                .active(request.getActive())
                .groupPoid(UserContext.getGroupPoid())
                .build();
    }

    public void updateEntity(AllowanceDeductionRequestDTO request, HrAllowanceDeductionMaster entity) {
        entity.setDescription(request.getDescription());
        entity.setVariableFixed(request.getVariableFixed());
        entity.setType(request.getType());
        entity.setFormula(request.getFormula());
        entity.setGlPoid(request.getGlPoid());
        entity.setMandatory(request.getMandatory() != null ? request.getMandatory() : "N");
        entity.setPayrollFieldName(request.getPayrollFieldName());
        entity.setSeqno(request.getSeqno());
        entity.setActive(request.getActive());
    }

    public AllowanceDeductionResponseDTO toResponseDTO(HrAllowanceDeductionMaster entity) {
        AllowanceDeductionResponseDTO dto = AllowanceDeductionResponseDTO.builder()
                .allowaceDeductionPoid(entity.getAllowaceDeductionPoid())
                .groupPoid(entity.getGroupPoid())
                .code(entity.getCode())
                .description(entity.getDescription())
                .variableFixed(entity.getVariableFixed())
                .type(entity.getType())
                .formula(entity.getFormula())
                .glcode(entity.getGlcode())
                .mandatory(entity.getMandatory())
                .active(entity.getActive())
                .seqno(entity.getSeqno())
                .deleted(entity.getDeleted())
                .glPoid(entity.getGlPoid())
                .payrollFieldName(entity.getPayrollFieldName())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();

        populateLovDetails(dto, entity);
        return dto;
    }

    private void populateLovDetails(AllowanceDeductionResponseDTO dto, HrAllowanceDeductionMaster entity) {
        try {
            if (entity.getVariableFixed() != null) {
                dto.setVariableFixedLov(lovService.getLovItemByCodeFast(entity.getVariableFixed(), "ALLOWANCE_CATEG"));
            }
            if (entity.getType() != null) {
                dto.setTypeLov(lovService.getLovItemByCodeFast(entity.getType(), "ALOWANCE_TYPE"));
            }
            if (entity.getGlPoid() != null) {
                dto.setGlLov(lovService.getDetailsByPoidAndLovName(entity.getGlPoid(), "GL_MASTER_LEDGERS"));
            }
        } catch (Exception e) {
            log.warn("Error fetching LOV details for allowance/deduction {}: {}", 
                    entity.getAllowaceDeductionPoid(), e.getMessage());
        }
    }
}
