package com.asg.hr.designation.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.designation.dto.DesignationRequest;
import com.asg.hr.designation.dto.DesignationResponse;
import com.asg.hr.designation.entity.HrDesignationMaster;
import com.asg.hr.designation.repository.DesignationRepository;
import com.asg.hr.exceptions.ValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository repository;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentSearchService;

    private static final String DESIGNATION = "Designation";
    private static final String DESIGNATION_POID = "DESIG_POID";
    private static final String DESIGNATIONPOID = "designationPoid";

    @Override
    public DesignationResponse getDesignationById(Long designationPoid) {
        HrDesignationMaster entity = repository.findById(designationPoid)
                .orElseThrow(() -> new ResourceNotFoundException(DESIGNATION, DESIGNATIONPOID, designationPoid));

        return mapToResponse(entity);
    }

    @Override
    public Long createDesignation(DesignationRequest request) {
        validateRequest(request, null);

        HrDesignationMaster entity = new HrDesignationMaster();
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setDesignationCode(request.getDesignationCode());
        entity.setDesignationName(request.getDesignationName());
        entity.setJobDescription(request.getJobDescription());
        entity.setSkillDescription(request.getSkillDescription());
        entity.setReportingToPoid(request.getReportingToPoid());
        entity.setSeqNo(request.getSeqNo());
        entity.setActive(StringUtils.defaultIfBlank(request.getActive(), "Y"));
        entity.setDeleted("N");

        repository.save(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(),
                entity.getDesignationPoid().toString());

        log.info("Created Designation with ID: {}", entity.getDesignationPoid());
        return entity.getDesignationPoid();
    }

    @Override
    public DesignationResponse updateDesignation(Long designationPoid, DesignationRequest request) {
        HrDesignationMaster entity = repository.findByDesignationPoidAndDeleted(designationPoid, "N")
                .orElseThrow(() -> new ResourceNotFoundException(DESIGNATION, DESIGNATIONPOID, designationPoid));

        validateRequest(request, designationPoid);

        HrDesignationMaster old = new HrDesignationMaster();
        BeanUtils.copyProperties(entity, old);

        entity.setDesignationCode(request.getDesignationCode());
        entity.setDesignationName(request.getDesignationName());
        entity.setJobDescription(request.getJobDescription());
        entity.setSkillDescription(request.getSkillDescription());
        entity.setReportingToPoid(request.getReportingToPoid());
        entity.setSeqNo(request.getSeqNo());
        entity.setActive(StringUtils.defaultIfBlank(request.getActive(), entity.getActive()));

        HrDesignationMaster updated = repository.save(entity);

        loggingService.logChanges(old, updated, HrDesignationMaster.class, UserContext.getDocumentId(),
                updated.getDesignationPoid().toString(), LogDetailsEnum.MODIFIED, DESIGNATION_POID);

        log.info("Updated Designation with ID: {}", updated.getDesignationPoid());
        return mapToResponse(updated);
    }

    @Override
    public void deleteDesignation(Long designationPoid, DeleteReasonDto deleteReasonDto) {
        repository.findById(designationPoid)
                .orElseThrow(() -> new ResourceNotFoundException(DESIGNATION, DESIGNATIONPOID, designationPoid));

        documentDeleteService.deleteDocument(designationPoid, "HR_DESIGNATION_MASTER", DESIGNATION_POID, deleteReasonDto, null);
        log.info("Soft deleted Designation with ID: {}", designationPoid);
    }

    @Override
    public Map<String, Object> listDesignations(FilterRequestDto filterRequest, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filters = documentSearchService.resolveFilters(filterRequest);

        RawSearchResult raw = documentSearchService.search(
                UserContext.getDocumentId(),
                filters,
                operator,
                pageable,
                isDeleted,
                "DESIGNATION_NAME",
                DESIGNATION_POID
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void validateRequest(DesignationRequest request, Long designationPoid) {
        if (request.getSeqNo() != null && request.getSeqNo() < 0) {
            throw new ValidationException("Sequence number must be numeric and non-negative");
        }

        checkDuplicate(
                repository.existsByDesignationCodeIgnoreCase(request.getDesignationCode()),
                () -> repository.existsByDesignationCodeIgnoreCaseAndDesignationPoidNot(request.getDesignationCode(), designationPoid),
                "Designation Code already exists: " + request.getDesignationCode()
        );

        checkDuplicate(
                repository.existsByDesignationNameIgnoreCase(request.getDesignationName()),
                () -> repository.existsByDesignationNameIgnoreCaseAndDesignationPoidNot(request.getDesignationName(), designationPoid),
                "Designation Name already exists: " + request.getDesignationName()
        );
    }

    private void checkDuplicate(boolean existsForCreate, java.util.function.Supplier<Boolean> existsForUpdateSupplier, String message) {
        boolean exists;
        if (existsForUpdateSupplier == null) {
            exists = existsForCreate;
        } else {
            exists = existsForUpdateSupplier.get();
        }
        if (exists) {
            throw new DuplicateKeyException(message);
        }
    }

    private DesignationResponse mapToResponse(HrDesignationMaster entity) {
        return DesignationResponse.builder()
                .designationPoid(entity.getDesignationPoid())
                .groupPoid(entity.getGroupPoid())
                .designationCode(entity.getDesignationCode())
                .designationName(entity.getDesignationName())
                .jobDescription(entity.getJobDescription())
                .skillDescription(entity.getSkillDescription())
                .reportingToPoid(entity.getReportingToPoid())
                .seqNo(entity.getSeqNo())
                .active(entity.getActive())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}

