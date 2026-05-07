package com.asg.hr.employeeinduction.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.CustomException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.employeeinduction.dto.EmployeeInductionRequestDto;
import com.asg.hr.employeeinduction.dto.EmployeeInductionResponseDto;
import com.asg.hr.employeeinduction.dto.InductionCategoryDto;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtl;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionDtlId;
import com.asg.hr.employeeinduction.entity.HrEmployeeInductionHdr;
import com.asg.hr.employeeinduction.repository.EmployeeInductionProcRepository;
import com.asg.hr.employeeinduction.repository.HrEmployeeInductionDtlRepository;
import com.asg.hr.employeeinduction.repository.HrEmployeeInductionHdrRepository;
import com.asg.hr.employeeinduction.util.EmployeeInductionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeInductionServiceImpl implements EmployeeInductionService {

    private static final String TABLE_NAME = "HR_EMPLOYEE_INDUCTION_HDR";
    private static final String PRIMARY_KEY = "TRANSACTION_POID";
    private static final String DESCRIPTION_FIELD = "REMARKS";
    private static final String ERROR_CANNOT_UPDATE_DELETED = "Cannot update deleted record";
    private static final String ERROR_RECORD_DELETED = "Record has been deleted";
    private static final String EMPLOYEE_INDUCTION = "Employee Induction";
    private static final String POID_FIELD = "transactionPoid";

    private final HrEmployeeInductionHdrRepository hdrRepository;
    private final HrEmployeeInductionDtlRepository dtlRepository;
    private final EmployeeInductionProcRepository procRepository;
    private final LoggingService loggingService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional
    public EmployeeInductionResponseDto createInduction(EmployeeInductionRequestDto requestDto) {
        log.info("Creating employee induction for employee: {}", requestDto.getEmployeePoid());
        validateRequest(requestDto);
        
        HrEmployeeInductionHdr header = HrEmployeeInductionHdr.builder()
                .docRef(requestDto.getDocId())
                .employeePoid(requestDto.getEmployeePoid())
                .remarks(requestDto.getRemarks())
                .companyPoid(UserContext.getCompanyPoid()) // Default company, should be from context
                .transactionDate(LocalDate.now())
                .build();

        header.setCreatedBy(ASGHelperUtils.getCurrentUser());
        header.setCreatedDate(LocalDateTime.now());
        header.setDeleted(EmployeeInductionConstants.DELETED_NO);

        // Save header first to get the generated poid
        header = hdrRepository.save(header);
        
        // Now create details with the saved header's poid
        if (requestDto.getDetails() != null && !requestDto.getDetails().isEmpty()) {
            List<HrEmployeeInductionDtl> details = createDetails(header, requestDto.getDetails());
            header.setDetails(details);
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), header.getPoid().toString());
        log.info("Successfully created employee induction with id: {}", header.getPoid());
        return mapToResponseDto(header);
    }

    @Override
    @Transactional
    public EmployeeInductionResponseDto updateInduction(Long poid, EmployeeInductionRequestDto requestDto) {
        log.info("Updating employee induction with id: {}", poid);
        
        HrEmployeeInductionHdr header = hdrRepository.findByPoidAndNotDeleted(poid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_INDUCTION, POID_FIELD, poid));

        if (EmployeeInductionConstants.DELETED_YES.equals(header.getDeleted())) {
            throw new CustomException(ERROR_CANNOT_UPDATE_DELETED);
        }

        HrEmployeeInductionHdr oldEntity = new HrEmployeeInductionHdr();
        BeanUtils.copyProperties(header, oldEntity);

        header.setDocRef(requestDto.getDocId());
        header.setEmployeePoid(requestDto.getEmployeePoid());
        header.setRemarks(requestDto.getRemarks());
        header.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        header.setLastModifiedDate(LocalDateTime.now());

        // Delete existing details
        List<HrEmployeeInductionDtl> existingDetails = dtlRepository.findByHdrPoidAndNotDeleted(poid);
        existingDetails.forEach(detail -> detail.setDeleted(EmployeeInductionConstants.DELETED_YES));
        dtlRepository.saveAll(existingDetails);

        // Create new details
        if (requestDto.getDetails() != null) {
            List<HrEmployeeInductionDtl> newDetails = createDetails(header, requestDto.getDetails());
            header.setDetails(newDetails);
        }

        header = hdrRepository.save(header);
        loggingService.logChanges(oldEntity, header, HrEmployeeInductionHdr.class,
                UserContext.getDocumentId(), poid.toString(), LogDetailsEnum.MODIFIED, PRIMARY_KEY);
        
        log.info("Successfully updated employee induction with id: {}", poid);
        return mapToResponseDto(header);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeInductionResponseDto getInductionById(Long poid) {
        log.info("Getting employee induction with id: {}", poid);
        
        HrEmployeeInductionHdr header = hdrRepository.findByPoidAndNotDeleted(poid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_INDUCTION, POID_FIELD, poid));
        
        if (EmployeeInductionConstants.DELETED_YES.equals(header.getDeleted())) {
            throw new CustomException(ERROR_RECORD_DELETED);
        }
        
        log.info("Successfully retrieved employee induction with id: {}", poid);
        return mapToResponseDto(header);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching employee inductions with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "CREATED_DATE", startDate, endDate);

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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getInductionsByEmployee(Long employeePoid) {
        log.info("Getting employee inductions for employee: {}", employeePoid);
        
        List<HrEmployeeInductionHdr> inductions = hdrRepository.findByEmployeePoidAndNotDeleted(employeePoid);
        List<EmployeeInductionResponseDto> responseDtos = inductions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("inductions", responseDtos);
        result.put("totalCount", responseDtos.size());
        
        log.info("Successfully retrieved {} employee inductions for employee: {}", responseDtos.size(), employeePoid);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> loadInductionByEmployee(Long employeePoid) {
        log.info("Loading employee induction for employee: {}", employeePoid);
        return getInductionsByEmployee(employeePoid);
    }

    @Override
    @Transactional
    public void deleteInduction(Long poid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting employee induction with id: {}", poid);
        
        hdrRepository.findByPoidAndNotDeleted(poid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_INDUCTION, POID_FIELD, poid));
        
        // Use DocumentDeleteService for deletion (handles logging internally)
        documentDeleteService.deleteDocument(
                poid,
                TABLE_NAME,
                PRIMARY_KEY,
                deleteReasonDto,
                null
        );
        
        log.info("Soft deleted employee induction with ID: {}", poid);
    }

    @Override
    public void sendOverdueNotifications() {
        log.info("Sending overdue notifications for employee inductions");
        
        List<HrEmployeeInductionDtl> overdueInductions = dtlRepository.findOverdueInductions(LocalDate.now());
        
        // Email notification logic would be implemented here
        // For now, just logging the overdue inductions
        for (HrEmployeeInductionDtl overdue : overdueInductions) {
            log.warn("Overdue induction activity - Category: {}, Employee: {}, Scheduled: {}", 
                    overdue.getInductionCategory(), 
                    overdue.getHeader().getEmployeePoid(), 
                    overdue.getScheduledDate());
        }
        
        log.info("Found {} overdue inductions", overdueInductions.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InductionCategoryDto> getInductionCategories() {
        log.info("Getting induction categories from stored procedure");
        
        try {
            List<Map<String, Object>> rawCategories = procRepository.getInductionCategories();
            
            List<InductionCategoryDto> categories = rawCategories.stream()
                    .map(this::mapToInductionCategoryDto)
                    .collect(Collectors.toList());
            
            log.info("Successfully retrieved {} induction categories", categories.size());
            return categories;
        } catch (Exception e) {
            log.error("Failed to retrieve induction categories", e);
            throw new RuntimeException("Failed to retrieve induction categories: " + e.getMessage(), e);
        }
    }

    private List<HrEmployeeInductionDtl> createDetails(HrEmployeeInductionHdr header, 
                                                      List<EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto> detailDtos) {
        return detailDtos.stream().map(detailDto -> {
            HrEmployeeInductionDtl detail = HrEmployeeInductionDtl.builder()
                    .transactionPoid(header.getTransactionPoid())
                    .detRowId(detailDto.getSn().longValue())
                    .header(header)
                    .inductionCatgPoid(detailDto.getInductionCategory() != null ? Long.parseLong(detailDto.getInductionCategory()) : 1L)
                    .sheduledDate(detailDto.getScheduledDate())
                    .compleatedDate(detailDto.getCompletedDate())
                    .status(detailDto.getStatus())
                    .remarks(detailDto.getRemarks())
                    .build();
            
            detail.setCreatedBy(ASGHelperUtils.getCurrentUser());
            detail.setCreatedDate(LocalDateTime.now());
            
            return dtlRepository.save(detail);
        }).collect(Collectors.toList());
    }

    private EmployeeInductionResponseDto mapToResponseDto(HrEmployeeInductionHdr header) {
        List<EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto> detailDtos = 
                dtlRepository.findByHdrPoidAndNotDeleted(header.getTransactionPoid()).stream()
                        .map(detail -> EmployeeInductionResponseDto.EmployeeInductionDetailResponseDto.builder()
                                .sn(detail.getDetRowId() != null ? detail.getDetRowId().intValue() : null)
                                .inductionCategory(detail.getInductionCatgPoid() != null ? detail.getInductionCatgPoid().toString() : null)
                                .assigneePoid(null) // Column doesn't exist
                                .scheduledDate(detail.getSheduledDate())
                                .completedDate(detail.getCompleatedDate())
                                .status(detail.getStatus())
                                .remarks(detail.getRemarks())
                                .build())
                        .collect(Collectors.toList());

        return EmployeeInductionResponseDto.builder()
                .poid(header.getPoid())
                .docId(header.getDocId())
                .employeePoid(header.getEmployeePoid())
                .remarks(header.getRemarks())
                .details(detailDtos)
                .build();
    }

    private void validateRequest(EmployeeInductionRequestDto requestDto) {
        if (requestDto.getEmployeePoid() == null) {
            throw new IllegalArgumentException("Employee is required");
        }
        
        if (requestDto.getDetails() != null) {
            for (EmployeeInductionRequestDto.EmployeeInductionDetailRequestDto detail : requestDto.getDetails()) {
                if (detail.getScheduledDate() != null && detail.getCompletedDate() != null) {
                    if (detail.getCompletedDate().isBefore(detail.getScheduledDate())) {
                        throw new IllegalArgumentException("Completed date cannot be earlier than scheduled date");
                    }
                }
            }
        }
    }

    private InductionCategoryDto mapToInductionCategoryDto(Map<String, Object> rawData) {
        return InductionCategoryDto.builder()
                .inductionCatgPoid(getLongValue(rawData, "INDUCTION_CATG_POID"))
                .status(getStringValue(rawData, "STATUS"))
                .description(getStringValue(rawData, "DESCRIPTION"))
                .seqNo(getIntegerValue(rawData, "CATG_SEQNO"))
                .build();
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Could not parse {} as Long: {}", key, value);
            return null;
        }
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Could not parse {} as Integer: {}", key, value);
            return null;
        }
    }
}