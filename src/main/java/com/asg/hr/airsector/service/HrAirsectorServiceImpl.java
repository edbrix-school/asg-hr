package com.asg.hr.airsector.service;

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
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.airsector.dto.HrAirsectorRequestDto;
import com.asg.hr.airsector.dto.HrAirsectorResponseDto;
import com.asg.hr.airsector.entity.HrAirsectorMaster;
import com.asg.hr.airsector.repository.HrAirsectorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class HrAirsectorServiceImpl implements HrAirsectorService {

    private static final String PRIMARY_KEY = "AIRSEC_POID";
    private static final String EXCEPTION_MESSAGE = "Airsector not found";

    private final HrAirsectorRepository repository;
    private final LoggingService loggingService;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final LovDataService lovService;



    @Override
    public HrAirsectorResponseDto create(HrAirsectorRequestDto request) {

        String description = request.getAirsectorDescription().trim().toUpperCase();

        if (repository.existsByAirsectorDescription(description)) {
            throw new ResourceAlreadyExistsException("airsectorDescription", description);
        }

        HrAirsectorMaster entity = HrAirsectorMaster.builder()
                .groupPoid(UserContext.getGroupPoid())
                .airsectorDescription(description)
                .active(request.getActive())
                .seqno(request.getSeqno())
                .averageTicketRate(request.getAverageTicketRate())
                .hrCountryPoid(request.getHrCountryPoid())
                .businessFare(request.getBusinessFare())
                .deleted("N")
                .active(request.getActive() != null ? request.getActive() : "Y")
                .build();

        HrAirsectorMaster savedEntity = repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = savedEntity.getAirsecPoid().toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        return mapToResponse(savedEntity);
    }

    @Override
    public HrAirsectorResponseDto update(Long poid, HrAirsectorRequestDto request) {

        HrAirsectorMaster entity = repository.findById(poid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        EXCEPTION_MESSAGE, PRIMARY_KEY, poid));

        String description = request.getAirsectorDescription().trim().toUpperCase();

        // Duplicate check (excluding current record)
        if (repository.existsByAirsectorDescriptionAndAirsecPoidNot(description, poid)) {
            throw new ResourceAlreadyExistsException("airsectorDescription", description);
        }

        HrAirsectorMaster oldEntity = new HrAirsectorMaster();
        BeanUtils.copyProperties(entity, oldEntity);

        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setAirsectorDescription(description);
        entity.setActive(request.getActive());
        entity.setSeqno(request.getSeqno());
        entity.setAverageTicketRate(request.getAverageTicketRate());
        entity.setHrCountryPoid(request.getHrCountryPoid());
        entity.setBusinessFare(request.getBusinessFare());

        HrAirsectorMaster updatedEntity = repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = updatedEntity.getAirsecPoid().toString();

        loggingService.logChanges(oldEntity, updatedEntity,
                HrAirsectorMaster.class, docId, key,
                LogDetailsEnum.MODIFIED, PRIMARY_KEY);

        return mapToResponse(updatedEntity);
    }

    @Override
    @Transactional
    public HrAirsectorResponseDto findById(Long poid) {

        return repository.findById(poid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        EXCEPTION_MESSAGE, PRIMARY_KEY, poid));
    }

    @Override
    public void deleteAirsectorMaster(Long airsecPoid,
                       DeleteReasonDto deleteReasonDto) {

       repository.findById(airsecPoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        EXCEPTION_MESSAGE, PRIMARY_KEY, airsecPoid));

        documentDeleteService.deleteDocument(
                airsecPoid,
                "HR_AIRSECTOR_MASTER",
                PRIMARY_KEY,
                deleteReasonDto,
                LocalDate.now()
        );
    }

    @Override
    public Map<String, Object> listOfRecordsAndGenericSearch(String docId,
                                                             FilterRequestDto request,
                                                             Pageable pageable,
                                                             LocalDate startDate,
                                                             LocalDate endDate) {

        String operator = documentService.resolveOperator(request);

        String isDeleted = documentService.resolveIsDeleted(request);

        List<FilterDto> filters = documentService.resolveDateFilters(
                request,
                "CREATED_DATE",
                startDate,
                endDate
        );

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "AIRSECTOR_CODE",
                PRIMARY_KEY
        );

        Page<Map<String, Object>> page =
                new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private HrAirsectorResponseDto mapToResponse(HrAirsectorMaster e) {

        return HrAirsectorResponseDto.builder()
                .airsecPoid(e.getAirsecPoid())
                .groupPoid(e.getGroupPoid())
                .airsectorCode(e.getAirsectorCode())
                .airsectorDescription(e.getAirsectorDescription())
                .active(e.getActive())
                .seqno(e.getSeqno())
                .averageTicketRate(e.getAverageTicketRate())
                .hrCountryPoid(e.getHrCountryPoid())
                .countryDtl(lovService.getDetailsByPoidAndLovNameFast(e.getHrCountryPoid(), "NATIONALITY"))
                .businessFare(e.getBusinessFare())
                .createdBy(e.getCreatedBy())
                .createdDate(e.getCreatedDate())
                .lastmodifiedBy(e.getLastModifiedBy())
                .lastmodifiedDate(e.getLastModifiedDate())
                .deleted(e.getDeleted())
                .build();
    }

}
