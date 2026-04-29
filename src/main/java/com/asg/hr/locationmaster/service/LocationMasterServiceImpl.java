package com.asg.hr.locationmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
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
import com.asg.hr.locationmaster.dto.LocationMasterRequestDto;
import com.asg.hr.locationmaster.dto.LocationMasterResponseDto;
import com.asg.hr.locationmaster.entity.GlobalLocationMaster;
import com.asg.hr.locationmaster.repository.GlobalLocationMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationMasterServiceImpl implements LocationMasterService {

    private static final String LOCATION_ENTITY_NAME = "GLOBAL_LOCATION_MASTER";
    private static final String ACTIVE_YES = "Y";
    private static final String DELETED_NO = "N";
    private static final String ID_FIELD = "LOCATION_POID";
    private static final String LOCATION_NAME_FIELD = "LOCATION_NAME";
    private static final String LOCATION_POID_FIELD = "LOCATION_POID";
    private static final String GLOBAL_LOCATION_MASTER_TABLE = "GLOBAL_LOCATION_MASTER";
    private static final String SUPERVISOR_NAME_LOV = "SUPERVISOR_NAME_FOR_LOC";

    private final GlobalLocationMasterRepository repository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final LovDataService lovDataService;

    @Override
    @Transactional
    public LocationMasterResponseDto create(LocationMasterRequestDto requestDto) {
        Long companyPoid = UserContext.getCompanyPoid();

        if (repository.existsByLocationCodeAndCompanyPoid(requestDto.getLocationCode(), companyPoid)) {
            throw new ResourceAlreadyExistsException(LOCATION_ENTITY_NAME, requestDto.getLocationCode());
        }

        GlobalLocationMaster entity = GlobalLocationMaster.builder()
                .companyPoid(companyPoid)
                .locationCode(requestDto.getLocationCode())
                .locationName(requestDto.getLocationName())
                .locationName2(requestDto.getLocationName2())
                .address(requestDto.getAddress())
                .siteSupervisorUserPoid(requestDto.getSiteSupervisorUserPoid())
                .seqno(requestDto.getSeqno())
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
    public LocationMasterResponseDto getById(Long locationPoid) {
        Long companyPoid = UserContext.getCompanyPoid();
        
        GlobalLocationMaster entity = repository.findByIdAndCompanyPoidAndNotDeleted(locationPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LOCATION_ENTITY_NAME, ID_FIELD, locationPoid));
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public LocationMasterResponseDto update(Long locationPoid, LocationMasterRequestDto requestDto) {
        Long companyPoid = UserContext.getCompanyPoid();

        GlobalLocationMaster entity = repository.findByIdAndCompanyPoidAndNotDeleted(locationPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LOCATION_ENTITY_NAME, ID_FIELD, locationPoid));

        if (repository.existsByLocationCodeAndCompanyPoidAndLocationPoidNot(requestDto.getLocationCode(), companyPoid, locationPoid)) {
            throw new ResourceAlreadyExistsException(LOCATION_ENTITY_NAME, requestDto.getLocationCode());
        }

        GlobalLocationMaster oldEntity = new GlobalLocationMaster();
        BeanUtils.copyProperties(entity, oldEntity);

        entity.setLocationCode(requestDto.getLocationCode());
        entity.setLocationName(requestDto.getLocationName());
        entity.setLocationName2(requestDto.getLocationName2());
        entity.setAddress(requestDto.getAddress());
        entity.setSiteSupervisorUserPoid(requestDto.getSiteSupervisorUserPoid());
        entity.setActive(requestDto.getActive() != null ? requestDto.getActive() : entity.getActive());
        entity.setSeqno(requestDto.getSeqno());

        entity = repository.save(entity);
        
        loggingService.logChanges(oldEntity, entity, GlobalLocationMaster.class, 
                UserContext.getDocumentId(), locationPoid.toString(), LogDetailsEnum.MODIFIED, LOCATION_POID_FIELD);
        
        return toResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long locationPoid, DeleteReasonDto deleteReasonDto) {
        Long companyPoid = UserContext.getCompanyPoid();

        repository.findByIdAndCompanyPoidAndNotDeleted(locationPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LOCATION_ENTITY_NAME, ID_FIELD, locationPoid));

        documentDeleteService.deleteDocument(
                locationPoid,
                GLOBAL_LOCATION_MASTER_TABLE,
                LOCATION_POID_FIELD,
                deleteReasonDto,
                null
        );
    }

    private LocationMasterResponseDto toResponseDto(GlobalLocationMaster entity) {
        LocationMasterResponseDto.LocationMasterResponseDtoBuilder builder = LocationMasterResponseDto.builder()
                .locationPoid(entity.getLocationPoid())
                .companyPoid(entity.getCompanyPoid())
                .locationCode(entity.getLocationCode())
                .locationName(entity.getLocationName())
                .locationName2(entity.getLocationName2())
                .address(entity.getAddress())
                .siteSupervisorUserPoid(entity.getSiteSupervisorUserPoid())
                .active(entity.getActive())
                .seqno(entity.getSeqno())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate());

        // Load site supervisor details if available
        if (entity.getSiteSupervisorUserPoid() != null) {
            try {
                LovGetListDto supervisorDetails = lovDataService.getDetailsByPoidAndLovName(
                        entity.getSiteSupervisorUserPoid(), SUPERVISOR_NAME_LOV);
                if (supervisorDetails != null && supervisorDetails.getPoid() != null) {
                    builder.siteSupervisorDet(supervisorDetails);
                }
            } catch (Exception e) {
                log.warn("Failed to load site supervisor details for poid: {}", entity.getSiteSupervisorUserPoid(), e);
            }
        }

        return builder.build();
    }
}