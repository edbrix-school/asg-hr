package com.asg.hr.competency.service;

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
import com.asg.hr.competency.dto.CompetencyMasterRequestDto;
import com.asg.hr.competency.dto.CompetencyMasterResponseDto;
import com.asg.hr.competency.entity.CompetencyMasterEntity;
import com.asg.hr.competency.repository.CompetencyMasterRepository;
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
public class CompetencyMasterServiceImpl implements CompetencyMasterService {

    private final CompetencyMasterRepository repository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @Override
    @Transactional
    public CompetencyMasterResponseDto create(CompetencyMasterRequestDto requestDto) {
        Long groupPoid = UserContext.getGroupPoid();

        if (repository.existsByCompetencyCodeAndGroupPoid(requestDto.getCompetencyCode(), groupPoid)) {
            throw new ResourceAlreadyExistsException("Competency", requestDto.getCompetencyCode());
        }

        CompetencyMasterEntity entity = CompetencyMasterEntity.builder()
                .groupPoid(groupPoid)
                .competencyCode(requestDto.getCompetencyCode())
                .competencyDescription(requestDto.getCompetencyDescription())
                .competencyNarration(requestDto.getCompetencyNarration())
                .seqNo(requestDto.getSeqNo())
                .build();

        entity.setActive(requestDto.getActive() != null ? requestDto.getActive() : "Y");
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
    public CompetencyMasterResponseDto getById(Long id) {
        CompetencyMasterEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));
        
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public CompetencyMasterResponseDto update(Long competencyPoid, CompetencyMasterRequestDto requestDto) {
        Long groupPoid = UserContext.getGroupPoid();

        CompetencyMasterEntity entity = repository.findByIdAndGroupPoidAndNotDeleted(competencyPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", competencyPoid));

        if (repository.existsByCompetencyCodeAndGroupPoidAndIdNot(requestDto.getCompetencyCode(), groupPoid, competencyPoid)) {
            throw new ResourceAlreadyExistsException("Competency", requestDto.getCompetencyCode());
        }

        CompetencyMasterEntity oldEntity = CompetencyMasterEntity.builder()
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
        entity.setActive(requestDto.getActive() != null ? requestDto.getActive() : entity.getActive());
        entity.setSeqNo(requestDto.getSeqNo());

        entity = repository.save(entity);
        
        loggingService.logChanges(oldEntity, entity, CompetencyMasterEntity.class, 
                UserContext.getDocumentId(), competencyPoid.toString(), LogDetailsEnum.MODIFIED, "COMPETENCY_POID");
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id, DeleteReasonDto deleteReasonDto) {
        Long groupPoid = UserContext.getGroupPoid();

        repository.findByIdAndGroupPoidAndNotDeleted(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Competency", "id", id));

        documentDeleteService.deleteDocument(
                id,
                "HR_COMPETENCY_MASTER",
                "COMPETENCY_POID",
                deleteReasonDto,
                null
        );
    }

    private CompetencyMasterResponseDto toResponseDto(CompetencyMasterEntity entity) {
        return CompetencyMasterResponseDto.builder()
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
