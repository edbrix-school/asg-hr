package com.asg.hr.departmentmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterRequest;
import com.asg.hr.departmentmaster.dto.HrDepartmentMasterResponse;
import com.asg.hr.departmentmaster.entity.HrDepartmentMaster;
import com.asg.hr.departmentmaster.repository.GlCostCenterMasterRepository;
import com.asg.hr.departmentmaster.repository.HrDepartmentMasterRepository;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HrDepartmentMasterServiceImpl implements HrDepartmentMasterService {

    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentSearchService;
    private final HrDepartmentMasterRepository repository;
    private final GlCostCenterMasterRepository glCostCenterMasterRepository;
    private final LoggingService loggingService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllDepartmentsWithFilters(String documentId, FilterRequestDto filterRequestDto, Pageable pageable) {

        String operator = documentSearchService.resolveOperator(filterRequestDto);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequestDto);
        List<FilterDto> filters = documentSearchService.resolveFilters(filterRequestDto);

        RawSearchResult raw = documentSearchService.search(documentId, filters, operator, pageable, isDeleted,
                "DEPT_NAME",
                "DEPT_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public HrDepartmentMasterResponse getDepartmentById(Long deptPoid) {
        HrDepartmentMaster entity = repository.findById(deptPoid).orElseThrow(() -> new ResourceNotFoundException("Department", "deptPoid", deptPoid));

        return mapToResponse(entity);
    }

    @Override
    public HrDepartmentMasterResponse createDepartment(HrDepartmentMasterRequest request, Long groupPoid, String userId) {
        if ("Y".equalsIgnoreCase(request.getSubdeptYN())) {
            if (request.getParentDeptPoid() == null || !repository.existsByDeptPoid(request.getParentDeptPoid())) {
                throw new ResourceNotFoundException("Parent department", "parentDeptPoid", request.getParentDeptPoid());
            }
        }
        if (StringUtils.isNotBlank(request.getDeptName()) && repository.existsByDeptNameIgnoreCase(request.getDeptName())) {
            throw new ResourceAlreadyExistsException("Department name", request.getDeptName());
        }
        if (!glCostCenterMasterRepository.existsByCostCenterPoid(request.getCostCentrePoid())) {
            throw new ResourceNotFoundException("Cost centre", "costCentrePoid", request.getCostCentrePoid());
        }

        HrDepartmentMaster entity = new HrDepartmentMaster();
        entity.setGroupPoid(groupPoid);
        entity.setDeptName(request.getDeptName());
        entity.setSubdeptYN(StringUtils.isNotBlank(request.getSubdeptYN()) ? request.getSubdeptYN() : "N");
        entity.setActive(StringUtils.isNotBlank(request.getActive()) ? request.getActive() : "Y");
        entity.setSeqNo(request.getSeqNo());
        entity.setParentDeptPoid(request.getParentDeptPoid());
        entity.setCostCentrePoid(request.getCostCentrePoid());
        entity.setDeleted("N");

        entity = repository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    public HrDepartmentMasterResponse updateDepartment(Long deptPoid, HrDepartmentMasterRequest request, Long groupPoid, String userId) {
        HrDepartmentMaster entity = repository.findById(deptPoid).orElseThrow(() -> new ResourceNotFoundException("Department", "deptPoid", deptPoid));

        if ("Y".equalsIgnoreCase(request.getSubdeptYN())) {
            if (request.getParentDeptPoid() == null || !repository.existsByDeptPoid(request.getParentDeptPoid())) {
                throw new ResourceNotFoundException("Parent department", "parentDeptPoid", request.getParentDeptPoid());
            }
        }
        if (StringUtils.isNotBlank(request.getDeptName()) && repository.existsByDeptNameIgnoreCaseAndDeptPoidNot(request.getDeptName(), deptPoid)) {
            throw new ResourceAlreadyExistsException("Department name", request.getDeptName());
        }
        if (!glCostCenterMasterRepository.existsByCostCenterPoid(request.getCostCentrePoid())) {
            throw new ResourceNotFoundException("Cost centre", "costCentrePoid", request.getCostCentrePoid());
        }
        if (deptPoid.equals(request.getParentDeptPoid())) {
            throw new ValidationException("Should not select current department as parent department");
        }

        entity.setDeptName(request.getDeptName());
        entity.setSubdeptYN(StringUtils.isNotBlank(request.getSubdeptYN()) ? request.getSubdeptYN() : entity.getSubdeptYN());
        entity.setActive(StringUtils.isNotBlank(request.getActive()) ? request.getActive() : entity.getActive());
        entity.setSeqNo(request.getSeqNo());
        entity.setParentDeptPoid(request.getParentDeptPoid());
        entity.setCostCentrePoid(request.getCostCentrePoid());

        entity = repository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    public void deleteDepartment(Long deptPoid, Long groupPoid, String userId, @Valid DeleteReasonDto deleteReasonDto) {
        repository.findById(deptPoid).orElseThrow(() -> new ResourceNotFoundException("Department", "deptPoid", deptPoid));

        documentDeleteService.deleteDocument(
                deptPoid,
                "HR_DEPARTMENT_MASTER",
                "DEPT_POID",
                deleteReasonDto,
                LocalDate.now()
        );
    }

    private HrDepartmentMasterResponse mapToResponse(HrDepartmentMaster entity) {
        return HrDepartmentMasterResponse.builder()
                .deptPoid(entity.getDeptPoid())
                .groupPoid(entity.getGroupPoid())
                .baseGroup(entity.getBaseGroup())
                .deptCode(entity.getDeptCode())
                .deptName(entity.getDeptName())
                .subdeptYN(entity.getSubdeptYN())
                .active(entity.getActive())
                .seqNo(entity.getSeqNo())
                .parentDeptPoid(entity.getParentDeptPoid())
                .costCentrePoid(entity.getCostCentrePoid())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}

