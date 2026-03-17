package com.asg.hr.allowanceanddeductionmaster.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.CustomException;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.allowanceanddeductionmaster.dto.*;
import com.asg.hr.allowanceanddeductionmaster.entity.HrAllowanceDeductionMaster;
import com.asg.hr.allowanceanddeductionmaster.mapper.AllowanceDeductionMasterMapper;
import com.asg.hr.allowanceanddeductionmaster.repository.AllowanceDeductionMasterRepository;
import com.asg.hr.allowanceanddeductionmaster.service.AllowanceDeductionMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AllowanceDeductionMasterServiceImpl implements AllowanceDeductionMasterService {

    private static final String DELETED_FLAG = "Y";
    private static final String NOT_DELETED_FLAG = "N";
    private static final String TABLE_NAME = "HR_ALLOWANCE_DEDUCTION_MASTER";
    private static final String PRIMARY_KEY = "ALLOWACE_DEDUCTION_POID";
    private static final String DESCRIPTION_FIELD = "DESCRIPTION";
    private static final String ERROR_CANNOT_UPDATE_DELETED = "Cannot update deleted record";
    private static final String ERROR_RECORD_DELETED = "Record has been deleted";
    private static final String ERROR_ALREADY_DELETED = "Cannot delete. Record is already deleted.";
    private static final String ALLOWANCE_DEDUCTION_CODE = "Allowance/Deduction code";
    private static final String ALLOWANCE_DEDUCTION = "Allowance/Deduction";
    private static final String PAYROLL_FIELD_NAME = "Payroll Field Name";
    private static final String ALLOWACE_DEDUCTION_POID_FIELD = "allowaceDeductionPoid";

    private final AllowanceDeductionMasterRepository repository;
    private final AllowanceDeductionMasterMapper mapper;
    private final LoggingService loggingService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    public AllowanceDeductionResponseDTO create(AllowanceDeductionRequestDTO request) {
        log.info("Creating allowance/deduction with code: {}", request.getCode());

        if (repository.findByCodeAndDeletedNot(request.getCode(), DELETED_FLAG).isPresent()) {
            throw new ResourceAlreadyExistsException(ALLOWANCE_DEDUCTION_CODE, request.getCode());
        }

        validatePayrollFieldName(request.getPayrollFieldName());

        HrAllowanceDeductionMaster entity = mapper.toEntity(request);
        entity.setCreatedBy(ASGHelperUtils.getCurrentUser());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setDeleted(NOT_DELETED_FLAG);

        HrAllowanceDeductionMaster saved = repository.save(entity);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getAllowaceDeductionPoid().toString());

        log.info("Successfully created allowance/deduction with id: {}", saved.getAllowaceDeductionPoid());
        return mapper.toResponseDTO(saved);
    }

    @Override
    public AllowanceDeductionResponseDTO update(Long allowaceDeductionPoid, AllowanceDeductionRequestDTO request) {
        log.info("Updating allowance/deduction with id: {}", allowaceDeductionPoid);

        HrAllowanceDeductionMaster entity = repository.findById(allowaceDeductionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ALLOWANCE_DEDUCTION, ALLOWACE_DEDUCTION_POID_FIELD, allowaceDeductionPoid));

        if (DELETED_FLAG.equals(entity.getDeleted())) {
            throw new CustomException(ERROR_CANNOT_UPDATE_DELETED);
        }

        validatePayrollFieldNameForUpdate(request.getPayrollFieldName(), allowaceDeductionPoid);

        HrAllowanceDeductionMaster oldEntity = new HrAllowanceDeductionMaster();
        BeanUtils.copyProperties(entity, oldEntity);

        mapper.updateEntity(request, entity);
        entity.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        HrAllowanceDeductionMaster saved = repository.save(entity);
        loggingService.logChanges(oldEntity, saved, HrAllowanceDeductionMaster.class,
                UserContext.getDocumentId(), allowaceDeductionPoid.toString(), LogDetailsEnum.MODIFIED, PRIMARY_KEY);

        log.info("Successfully updated allowance/deduction with id: {}", allowaceDeductionPoid);
        return mapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AllowanceDeductionResponseDTO getById(Long allowaceDeductionPoid) {
        log.info("Getting allowance/deduction with id: {}", allowaceDeductionPoid);

        HrAllowanceDeductionMaster entity = repository.findById(allowaceDeductionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ALLOWANCE_DEDUCTION, ALLOWACE_DEDUCTION_POID_FIELD, allowaceDeductionPoid));

        if (DELETED_FLAG.equals(entity.getDeleted())) {
            throw new CustomException(ERROR_RECORD_DELETED);
        }

        log.info("Successfully retrieved allowance/deduction with id: {}", allowaceDeductionPoid);
        return mapper.toResponseDTO(entity);
    }

    @Override
    public void delete(Long allowaceDeductionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting allowance/deduction with id: {}", allowaceDeductionPoid);

        HrAllowanceDeductionMaster entity = repository.findById(allowaceDeductionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ALLOWANCE_DEDUCTION, ALLOWACE_DEDUCTION_POID_FIELD, allowaceDeductionPoid));

        if (DELETED_FLAG.equals(entity.getDeleted())) {
            throw new CustomException(ERROR_ALREADY_DELETED);
        }

        documentDeleteService.deleteDocument(allowaceDeductionPoid, TABLE_NAME, PRIMARY_KEY, deleteReasonDto, null);
        log.info("Successfully deleted allowance/deduction with id: {}", allowaceDeductionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> search(FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching allowance/deductions with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(
                UserContext.getDocumentId(),
                filters,
                operator,
                pageable,
                isDeleted,
                DESCRIPTION_FIELD,
                PRIMARY_KEY
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void validatePayrollFieldName(String payrollFieldName) {
        if (payrollFieldName == null || payrollFieldName.isEmpty()) {
            return;
        }

        repository.findByPayrollFieldName(payrollFieldName)
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException(PAYROLL_FIELD_NAME, payrollFieldName);
                });
    }

    private void validatePayrollFieldNameForUpdate(String payrollFieldName, Long allowaceDeductionPoid) {
        if (payrollFieldName == null || payrollFieldName.isEmpty()) {
            return;
        }

        repository.findByPayrollFieldName(payrollFieldName)
                .ifPresent(existing -> {
                    if (!existing.getAllowaceDeductionPoid().equals(allowaceDeductionPoid)) {
                        throw new ResourceAlreadyExistsException(PAYROLL_FIELD_NAME, payrollFieldName);
                    }
                });
    }

}
