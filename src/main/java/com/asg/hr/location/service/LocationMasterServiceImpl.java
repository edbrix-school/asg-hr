package com.asg.hr.location.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.location.dto.LocationMasterRequestDto;
import com.asg.hr.location.dto.LocationMasterResponseDto;
import com.asg.hr.location.entity.LocationMasterEntity;
import com.asg.hr.location.repository.LocationMasterRepository;
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
public class LocationMasterServiceImpl implements LocationMasterService {

    private static final String LOCATION_CODE_SPACES_ERROR = "Location code cannot contain spaces";
    private static final String LOCATION_ENTITY_NAME = "GLOBAL_LOCATION_MASTER";
    private static final String LOCATION_CODE_FIELD = "LOCATION_CODE";
    private static final String ID_FIELD = "id";
    private static final String ACTIVE_YES = "Y";
    private static final String DELETED_NO = "N";
    private static final String LOCATION_NAME_FIELD = "LOCATION_NAME";
    private static final String LOCATION_POID_FIELD = "LOCATION_POID";
    private static final String GLOBAL_LOCATION_MASTER_TABLE = "GLOBAL_LOCATION_MASTER";

    private final LocationMasterRepository repository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @Override
    @Transactional
    public LocationMasterResponseDto create(LocationMasterRequestDto requestDto) {

        if (requestDto.getLocationCode() != null && requestDto.getLocationCode().trim().contains(" ")) {
            throw new ValidationException(LOCATION_CODE_SPACES_ERROR);
        }

        if (repository.existsByLocationCode(requestDto.getLocationCode())) {
            throw new ResourceAlreadyExistsException(LOCATION_CODE_FIELD, requestDto.getLocationCode());
        }

        LocationMasterEntity entity = LocationMasterEntity.builder()
                .company(UserContext.getCompanyPoid())
                .locationCode(requestDto.getLocationCode())
                .locationName(requestDto.getLocationName())
                .locationName2(requestDto.getLocationName2())
                .address(requestDto.getAddress())
                .siteSupervisorUserPoid(requestDto.getSiteSupervisorUserPoid())
                .build();

        entity.setActive(requestDto.getActive() != null ? requestDto.getActive() : ACTIVE_YES);
        entity.setDeleted(DELETED_NO);

        entity = repository.save(entity);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), entity.getLocationPoid().toString());
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(String docId, FilterRequestDto request, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(docId, filters, operator, pageable, isDeleted,
                LOCATION_NAME_FIELD,
                LOCATION_POID_FIELD);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LocationMasterResponseDto getById(Long id) {
        LocationMasterEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOCATION_ENTITY_NAME, ID_FIELD, id));
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public LocationMasterResponseDto update(Long locationPoid, LocationMasterRequestDto requestDto) {

        LocationMasterEntity entity = repository.findByIdAndNotDeleted(locationPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LOCATION_ENTITY_NAME, ID_FIELD, locationPoid));

        if (requestDto.getLocationCode() != null && requestDto.getLocationCode().trim().contains(" ")) {
            throw new ValidationException(LOCATION_CODE_SPACES_ERROR);
        }

        if (repository.existsByLocationCodeAndIdNot(requestDto.getLocationCode(),  locationPoid)) {
            throw new ResourceAlreadyExistsException(LOCATION_ENTITY_NAME, requestDto.getLocationCode());
        }

        LocationMasterEntity oldEntity = LocationMasterEntity.builder()
                .locationPoid(entity.getLocationPoid())
                .locationCode(entity.getLocationCode())
                .locationName(entity.getLocationName())
                .locationName2(entity.getLocationName2())
                .address(entity.getAddress())
                .siteSupervisorUserPoid(entity.getSiteSupervisorUserPoid())
                .build();
        oldEntity.setActive(entity.getActive());

        entity.setLocationCode(requestDto.getLocationCode());
        entity.setLocationName(requestDto.getLocationName());
        entity.setLocationName2(requestDto.getLocationName2());
        entity.setAddress(requestDto.getAddress());
        entity.setSiteSupervisorUserPoid(requestDto.getSiteSupervisorUserPoid());
        entity.setActive(requestDto.getActive() != null ? requestDto.getActive() : entity.getActive());

        entity = repository.save(entity);
        
        loggingService.logChanges(oldEntity, entity, LocationMasterEntity.class, 
                UserContext.getDocumentId(), locationPoid.toString(), LogDetailsEnum.MODIFIED, LOCATION_POID_FIELD);
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long id, DeleteReasonDto deleteReasonDto) {

        repository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOCATION_ENTITY_NAME, ID_FIELD, id));

        documentDeleteService.deleteDocument(
                id,
                GLOBAL_LOCATION_MASTER_TABLE,
                LOCATION_POID_FIELD,
                deleteReasonDto,
                null
        );
    }

    private LocationMasterResponseDto toResponseDto(LocationMasterEntity entity) {
        return LocationMasterResponseDto.builder()
                .locationPoid(entity.getLocationPoid())
                .locationCode(entity.getLocationCode())
                .locationName(entity.getLocationName())
                .locationName2(entity.getLocationName2())
                .address(entity.getAddress())
                .siteSupervisorUserPoid(entity.getSiteSupervisorUserPoid())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}