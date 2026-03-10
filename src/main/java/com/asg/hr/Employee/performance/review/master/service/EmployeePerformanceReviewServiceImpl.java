package com.asg.hr.Employee.performance.review.master.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.Employee.performance.review.master.dto.EmployeePerformanceReviewRequestDto;
import com.asg.hr.Employee.performance.review.master.dto.EmployeePerformanceReviewResponseDto;
import com.asg.hr.Employee.performance.review.master.entity.EmployeePerformanceReviewEntity;
import com.asg.hr.Employee.performance.review.master.repository.EmployeePerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeePerformanceReviewServiceImpl implements EmployeePerformanceReviewService {

    private final EmployeePerformanceReviewRepository repository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @Override
    @Transactional
    public EmployeePerformanceReviewResponseDto create(EmployeePerformanceReviewRequestDto requestDto) {
        Long groupPoid = UserContext.getGroupPoid();

        if (repository.existsByCompetencyCodeAndGroupPoid(requestDto.getCompetencyCode(), groupPoid)) {
            throw new ResourceAlreadyExistsException("Competency", requestDto.getCompetencyCode());
        }

        EmployeePerformanceReviewEntity entity = EmployeePerformanceReviewEntity.builder()
                .groupPoid(groupPoid)
                .competencyCode(requestDto.getCompetencyCode())
                .competencyDescription(requestDto.getCompetencyDescription())
                .competencyNarration(requestDto.getCompetencyNarration())
                .seqNo(requestDto.getSeqNo())
                .build();

        entity.setActive("Y");
        entity.setDeleted("N");

        entity = repository.save(entity);
        
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), entity.getCompetencyPoid().toString());
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(docId, filters, operator, pageable, isDeleted,
                "COMPETENCY_DESCRIPTION",
                "COMPETENCY_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeePerformanceReviewResponseDto getById(Long id) {
        Long groupPoid = UserContext.getGroupPoid();
        EmployeePerformanceReviewEntity entity = repository.findByIdAndGroupPoidAndNotDeleted(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public EmployeePerformanceReviewResponseDto update(Long competencyPoid, EmployeePerformanceReviewRequestDto requestDto) {
        Long groupPoid = UserContext.getGroupPoid();

        EmployeePerformanceReviewEntity entity = repository.findByIdAndGroupPoidAndNotDeleted(competencyPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", competencyPoid));

        if (repository.existsByCompetencyCodeAndGroupPoidAndIdNot(requestDto.getCompetencyCode(), groupPoid, competencyPoid)) {
            throw new ResourceAlreadyExistsException("Competency", requestDto.getCompetencyCode());
        }

        EmployeePerformanceReviewEntity oldEntity = EmployeePerformanceReviewEntity.builder()
                .competencyPoid(entity.getCompetencyPoid())
                .groupPoid(entity.getGroupPoid())
                .competencyCode(entity.getCompetencyCode())
                .competencyDescription(entity.getCompetencyDescription())
                .competencyNarration(entity.getCompetencyNarration())
                .seqNo(entity.getSeqNo())
                .build();
        oldEntity.setActive(entity.getActive());

        entity.setCompetencyCode(requestDto.getCompetencyCode());
        entity.setCompetencyDescription(requestDto.getCompetencyDescription());
        entity.setCompetencyNarration(requestDto.getCompetencyNarration());
        entity.setSeqNo(requestDto.getSeqNo());

        entity = repository.save(entity);
        
        loggingService.logChanges(oldEntity, entity, EmployeePerformanceReviewEntity.class, 
                UserContext.getDocumentId(), competencyPoid.toString(), LogDetailsEnum.MODIFIED, "COMPETENCY_POID");
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id, DeleteReasonDto deleteReasonDto) {
        Long groupPoid = UserContext.getGroupPoid();

        EmployeePerformanceReviewEntity entity = repository.findByIdAndGroupPoidAndNotDeleted(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));

        documentDeleteService.deleteDocument(
                id,
                "HR_COMPETENCY_MASTER",
                "COMPETENCY_POID",
                deleteReasonDto,
                null
        );
    }

    private EmployeePerformanceReviewResponseDto toResponseDto(EmployeePerformanceReviewEntity entity) {
        return EmployeePerformanceReviewResponseDto.builder()
                .competencyPoid(entity.getCompetencyPoid())
                .groupPoid(entity.getGroupPoid())
                .competencyCode(entity.getCompetencyCode())
                .competencyDescription(entity.getCompetencyDescription())
                .competencyNarration(entity.getCompetencyNarration())
                .active(entity.getActive())
                .seqNo(entity.getSeqNo())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
