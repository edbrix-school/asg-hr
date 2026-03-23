package com.asg.hr.nationality.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.nationality.dto.request.HrNationalityRequest;
import com.asg.hr.nationality.dto.request.HrNationalityUpdateRequest;
import com.asg.hr.nationality.dto.response.HrNationalityResponse;
import com.asg.hr.nationality.entity.HrNationalityMaster;
import com.asg.hr.nationality.repository.HrNationalityRepository;
import com.asg.hr.nationality.service.HrNationalityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getGroupId;

@Service
@Slf4j
@RequiredArgsConstructor
public class HrNationalityServiceImpl implements HrNationalityService {

    private final HrNationalityRepository hrNationalityRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService deleteService;
    private final LoggingService loggingService;

    private static final String FLAG_YES = "Y";
    private static final String FLAG_NO = "N";
    private static final String NATION_POID = "NATION_POID";
    private static final String DOC_ID = "800-004";

    @Override
    @Transactional
    public HrNationalityResponse create(HrNationalityRequest request) {
        validateRequest(request);

        HrNationalityMaster entity = new HrNationalityMaster();
        entity.setGroupPoid(getGroupId());
        entity.setNationalityCode(request.getNationalityCode());
        entity.setNationalityDescription(request.getNationalityDescription());
        entity.setTicketAmountNormal(request.getTicketAmountNormal());
        entity.setTicketAmountBusiness(request.getTicketAmountBusiness());
        entity.setActive(Boolean.TRUE.equals(request.getActive()) ? FLAG_YES : FLAG_NO);
        entity.setSeqno(request.getSeqNo());
        entity.setDeleted(FLAG_NO);

        entity = hrNationalityRepository.save(entity);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, DOC_ID,
                entity.getNationPoid().toString());
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public HrNationalityResponse update(Long nationPoid, HrNationalityUpdateRequest request) {
        if (nationPoid == null) {
            throw new ValidationException("Nation POID is required");
        }

        HrNationalityMaster existingEntity = findEntityById(nationPoid);
        validateUpdateRequest(request, existingEntity);


        HrNationalityMaster oldEntity = new HrNationalityMaster();
        BeanUtils.copyProperties(existingEntity, oldEntity);


        existingEntity.setNationalityDescription(request.getNationalityDescription() != null ? request.getNationalityDescription().trim() : null);
        existingEntity.setTicketAmountNormal(request.getTicketAmountNormal());
        existingEntity.setTicketAmountBusiness(request.getTicketAmountBusiness());
        existingEntity.setActive(Boolean.TRUE.equals(request.getActive()) ? FLAG_YES : FLAG_NO);
        existingEntity.setSeqno(request.getSeqNo());

        existingEntity = hrNationalityRepository.save(existingEntity);
        loggingService.logChanges(oldEntity, existingEntity, HrNationalityMaster.class, DOC_ID, nationPoid.toString(), LogDetailsEnum.MODIFIED, NATION_POID);

        return mapToResponse(existingEntity);
    }

    @Override
    public HrNationalityResponse getById(Long nationPoid) {
        HrNationalityMaster entity = findEntityById(nationPoid);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Long nationPoid, DeleteReasonDto deleteReasonDto) {
        findEntityById(nationPoid);
        deleteService.deleteDocument(nationPoid, "HR_NATIONALITY_MASTER",
                NATION_POID, deleteReasonDto, null);
    }

    @Override
    public Map<String, Object> list(FilterRequestDto filters, Pageable pageable) {
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentService.resolveFilters(filters);

        RawSearchResult raw = documentService.search(
                UserContext.getDocumentId(),
                filterList,
                operator,
                pageable,
                isDeleted,
                "NATIONALITY_DESCRIPTION",
                NATION_POID);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private HrNationalityMaster findEntityById(Long nationPoid) {
        return hrNationalityRepository.findById(nationPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Hr Nationality", "nationPoid", nationPoid));
    }

    private HrNationalityResponse mapToResponse(HrNationalityMaster entity) {
        return HrNationalityResponse.builder()
                .nationPoid(entity.getNationPoid())
                .nationalityCode(entity.getNationalityCode())
                .nationalityDescription(entity.getNationalityDescription())
                .ticketAmountNormal(entity.getTicketAmountNormal())
                .ticketAmountBusiness(entity.getTicketAmountBusiness())
                .active(FLAG_YES.equals(entity.getActive()))
                .seqNo(entity.getSeqno())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private void validateRequest(HrNationalityRequest request) {
        if (request.getNationalityCode() == null || request.getNationalityCode().trim().isEmpty()) {
            throw new ValidationException("Nationality Code is required");
        }

        String code = request.getNationalityCode().trim();
        if (hrNationalityRepository.existsByNationalityCode(code)) {
            throw new DuplicateKeyException("Nationality Code already exists: " + code);
        }

        if (request.getNationalityDescription() != null && !request.getNationalityDescription().trim().isEmpty()) {
            String desc = request.getNationalityDescription().trim();
            if (hrNationalityRepository.existsByNationalityDescription(desc)) {
                throw new DuplicateKeyException("Nationality Description already exists: " + desc);
            }
        }
    }

    private void validateUpdateRequest(HrNationalityUpdateRequest request, HrNationalityMaster existingEntity) {
        if (request.getNationalityDescription() != null && !request.getNationalityDescription().trim().isEmpty()) {
            String desc = request.getNationalityDescription().trim();
            if (hrNationalityRepository.existsByNationalityDescriptionExcluding(desc, existingEntity.getNationPoid())) {
                throw new DuplicateKeyException("Nationality Description already exists: " + desc);
            }
        }
    }
}
