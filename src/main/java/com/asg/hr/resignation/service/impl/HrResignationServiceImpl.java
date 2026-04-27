package com.asg.hr.resignation.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.resignation.dto.*;
import com.asg.hr.resignation.entity.HrResignationEntity;
import com.asg.hr.resignation.repository.HrResignationProcRepository;
import com.asg.hr.resignation.repository.HrResignationRepository;
import com.asg.hr.resignation.service.HrResignationService;
import com.asg.hr.resignation.util.HrResignationConstants;
import com.asg.hr.resignation.util.HrResignationMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrResignationServiceImpl implements HrResignationService {

    private final HrResignationRepository repository;
    private final HrResignationProcRepository procRepository;
    private final HrResignationMapper mapper;

    private final DocumentSearchService documentService;
    private final EntityManager entityManager;
    private final LovDataService lovDataService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listResignations(
            FilterRequestDto filters,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        String docId = getRequiredDocumentId();
        String operator = documentService.resolveOperator(filters);
        String isDeleted = documentService.resolveIsDeleted(filters);
        List<FilterDto> resolvedFilters = documentService.resolveDateFilters(
                filters,
                "TRANSACTION_DATE",
                startDate,
                endDate
        );

        RawSearchResult raw = documentService.search(
                docId,
                resolvedFilters,
                operator,
                pageable,
                isDeleted,
                "DOC_REF",
                HrResignationConstants.KEY_FIELD
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public HrResignationResponse getById(Long transactionPoid) {
        HrResignationEntity entity =
                repository.findById(transactionPoid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                HrResignationConstants.TABLE_NAME,
                                HrResignationConstants.KEY_FIELD,
                                transactionPoid
                        ));

        HrResignationResponse response = mapper.toResponse(entity);
        enrichLovDetails(response);
        return response;
    }

    @Override
    @Transactional
    public HrResignationResponse create(HrResignationRequest request) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        String docId = getRequiredDocumentId();

        if (groupPoid == null || companyPoid == null || userPoid == null) {
            throw new ValidationException("User not authenticated");
        }

        LocalDate docDate = request.getTransactionDate();
        LocalDate lastDateOfWork = request.getLastDateOfWork();

        if (lastDateOfWork != null && lastDateOfWork.isBefore(docDate)) {
            throw new ValidationException("Last date of work should not be before document date.");
        }

        HrResignationEmployeeDetailsResponse employeeDetails =
                procRepository.getEmployeeDetails(request.getEmployeePoid());

        String resignationType = request.getResignationType().trim().toUpperCase(Locale.ROOT);

        HrResignationEntity entity = mapper.toEntity(request);
        entity.setGroupPoid(groupPoid);
        entity.setCompanyPoid(companyPoid);
        entity.setTransactionDate(docDate);

        entity.setDepartmentPoid(employeeDetails.getDepartmentPoid());
        entity.setDesignationPoid(employeeDetails.getDesignationPoid());
        entity.setDirectSupervisorPoid(employeeDetails.getDirectSupervisorPoid());
        entity.setJoinDate(employeeDetails.getJoinDate());
        entity.setRpExpiryDate(employeeDetails.getRpExpiryDate());
        entity.setResignationType(resignationType);
        entity.setEmployeePoid(request.getEmployeePoid());

        String status = procRepository.beforeSaveValidation(
                companyPoid,
                userPoid,
                docDate,
                0L,
                request.getEmployeePoid(),
                lastDateOfWork
        );
        if (status != null && (status.contains(HrResignationConstants.STATUS_ERROR) || status.contains("WARNING"))) {
            throw new ValidationException(status);
        }

        HrResignationEntity saved = repository.save(entity);

        String key = saved.getTransactionPoid() != null ? saved.getTransactionPoid().toString() : "";
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        HrResignationResponse response = mapper.toResponse(saved);
        enrichLovDetails(response);
        return response;
    }

    @Override
    @Transactional
    public HrResignationResponse update(Long transactionPoid, HrResignationRequest request) {
        HrResignationEntity existing =
                repository.findByTransactionPoidAndDeletedNot(transactionPoid, "Y")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                HrResignationConstants.TABLE_NAME,
                                HrResignationConstants.KEY_FIELD,
                                transactionPoid
                        ));

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        String docId = getRequiredDocumentId();
        if (groupPoid == null || companyPoid == null || userPoid == null) {
            throw new ValidationException("User not authenticated");
        }

        LocalDate docDate = request.getTransactionDate();
        LocalDate lastDateOfWork = request.getLastDateOfWork();

        if (lastDateOfWork != null && docDate != null && lastDateOfWork.isBefore(docDate)) {
            throw new ValidationException("Last date of work should not be before document date.");
        }

        HrResignationEntity oldEntity = new HrResignationEntity();
        BeanUtils.copyProperties(existing, oldEntity);

        HrResignationEmployeeDetailsResponse employeeDetails = null;
        boolean employeeChanged = existing.getEmployeePoid() != null
                && request.getEmployeePoid() != null
                && !existing.getEmployeePoid().equals(request.getEmployeePoid());

        if (employeeChanged) {
            employeeDetails = procRepository.getEmployeeDetails(request.getEmployeePoid());
        }

        String resignationType = request.getResignationType().trim().toUpperCase(Locale.ROOT);

        existing.setLastDateOfWork(lastDateOfWork);
        existing.setTransactionDate(docDate);
        existing.setResignationDetails(request.getResignationDetails() != null ? request.getResignationDetails().trim() : null);
        existing.setResignationType(resignationType);
        existing.setHodRemarks(request.getHodRemarks() != null ? request.getHodRemarks().trim() : null);
        existing.setRemarks(request.getRemarks() != null ? request.getRemarks().trim() : null);
        existing.setEmployeePoid(request.getEmployeePoid());

        if (employeeChanged && employeeDetails != null) {
            existing.setDepartmentPoid(employeeDetails.getDepartmentPoid());
            existing.setDesignationPoid(employeeDetails.getDesignationPoid());
            existing.setDirectSupervisorPoid(employeeDetails.getDirectSupervisorPoid());
            existing.setJoinDate(employeeDetails.getJoinDate());
            existing.setRpExpiryDate(employeeDetails.getRpExpiryDate());
        }

        String status = procRepository.beforeSaveValidation(
                companyPoid,
                userPoid,
                docDate,
                transactionPoid,
                request.getEmployeePoid(),
                lastDateOfWork
        );
        if (status != null && (status.contains(HrResignationConstants.STATUS_ERROR) || status.contains("WARNING"))) {
            throw new ValidationException(status);
        }

        HrResignationEntity saved = repository.save(existing);

        String key = transactionPoid.toString();
        loggingService.logChanges(
                oldEntity,
                saved,
                HrResignationEntity.class,
                docId,
                key,
                LogDetailsEnum.MODIFIED,
                HrResignationConstants.KEY_FIELD
        );

        HrResignationResponse response = mapper.toResponse(saved);
        enrichLovDetails(response);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        HrResignationEntity entity =
                repository.findByTransactionPoidAndDeletedNot(transactionPoid, "Y")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                HrResignationConstants.TABLE_NAME,
                                HrResignationConstants.KEY_FIELD,
                                transactionPoid
                        ));

        String docIdForLogging = getRequiredDocumentId();
        loggingService.logDelete(entity, docIdForLogging, String.valueOf(transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                HrResignationConstants.TABLE_NAME,
                HrResignationConstants.KEY_FIELD,
                deleteReasonDto,
                LocalDate.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HrResignationEmployeeDetailsResponse getEmployeeDetails(Long employeePoid) {
        HrResignationEmployeeDetailsResponse details = procRepository.getEmployeeDetails(employeePoid);
        validateEmployeeDetails(details);
        enrichEmployeeDetailsLov(details);
        return details;
    }

    private void validateEmployeeDetails(HrResignationEmployeeDetailsResponse employeeDetails) {
        if (employeeDetails == null) {
            throw new ValidationException("Unable to fetch employee details");
        }
        if (employeeDetails.getStatus() == null) {
            throw new ValidationException("Employee details status is empty");
        }
        if (employeeDetails.getStatus().contains(HrResignationConstants.STATUS_ERROR) || !employeeDetails.getStatus().contains("SUCCESS")) {
            throw new ValidationException(employeeDetails.getStatus());
        }
    }

    private String getRequiredDocumentId() {
        String docId = UserContext.getDocumentId();
        if (docId == null || docId.isBlank()) {
            throw new ValidationException("Document id is mandatory");
        }
        return docId;
    }

    private void enrichLovDetails(HrResignationResponse response) {
        if (response == null) {
            return;
        }

        Map<Long, LovGetListDto> employeeLovMap = fetchEmployeeDetails(
                toDistinctPositiveList(response.getEmployeePoid(), response.getDirectSupervisorPoid())
        );
        Map<Long, LovGetListDto> departmentLovMap = fetchDepartmentDetails(
                toDistinctPositiveList(response.getDepartmentPoid())
        );
        Map<Long, LovGetListDto> designationLovMap = fetchDesignationDetails(
                toDistinctPositiveList(response.getDesignationPoid())
        );

        response.setEmployeeDet(getLovOrEmpty(employeeLovMap, response.getEmployeePoid()));
        response.setDepartmentDet(getLovOrEmpty(departmentLovMap, response.getDepartmentPoid()));
        response.setDesignationDet(getLovOrEmpty(designationLovMap, response.getDesignationPoid()));
        response.setDirectSupervisorDet(getLovOrEmpty(employeeLovMap, response.getDirectSupervisorPoid()));
        response.setResignationTypeDet(resolveResignationTypeLov(response.getResignationType()));
    }

    private void enrichEmployeeDetailsLov(HrResignationEmployeeDetailsResponse response) {
        if (response == null) {
            return;
        }
        Map<Long, LovGetListDto> employeeLovMap = fetchEmployeeDetails(
                toDistinctPositiveList(response.getDirectSupervisorPoid())
        );
        Map<Long, LovGetListDto> departmentLovMap = fetchDepartmentDetails(
                toDistinctPositiveList(response.getDepartmentPoid())
        );
        Map<Long, LovGetListDto> designationLovMap = fetchDesignationDetails(
                toDistinctPositiveList(response.getDesignationPoid())
        );
        response.setDepartmentDet(getLovOrEmpty(departmentLovMap, response.getDepartmentPoid()));
        response.setDesignationDet(getLovOrEmpty(designationLovMap, response.getDesignationPoid()));
        response.setDirectSupervisorDet(getLovOrEmpty(employeeLovMap, response.getDirectSupervisorPoid()));
        response.setResignationTypeDet(resolveResignationTypeLov(response.getResignationType()));
    }

    private LovGetListDto getLovOrEmpty(Map<Long, LovGetListDto> lovMap, Long poid) {
        if (poid == null || lovMap == null || lovMap.isEmpty()) {
            return new LovGetListDto();
        }
        return lovMap.getOrDefault(poid, new LovGetListDto());
    }

    private Map<Long, LovGetListDto> fetchEmployeeDetails(List<Long> employeePoids) {
        if (employeePoids.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT EMPLOYEE_POID AS POID,
                       EMPLOYEE_CODE AS CODE,
                       DISPLAY_NAME || '(' || EMPLOYEE_NAME || ' ' || EMPLOYEE_NAME2 || ')' AS DESCRIPTION
                  FROM HR_EMPLOYEE_MASTER
                 WHERE EMPLOYEE_POID IN (:poids)
                 ORDER BY SEQNO
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(HrResignationConstants.PARAM_POIDS, employeePoids);
        return toLovMap(query);
    }

    private Map<Long, LovGetListDto> fetchDepartmentDetails(List<Long> departmentPoids) {
        if (departmentPoids.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT DEPT_POID AS POID,
                       DEPT_CODE AS CODE,
                       DEPT_NAME AS DESCRIPTION
                  FROM HR_DEPARTMENT_MASTER
                 WHERE DEPT_POID IN (:poids)
                 ORDER BY SEQNO
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(HrResignationConstants.PARAM_POIDS, departmentPoids);
        return toLovMap(query);
    }

    private Map<Long, LovGetListDto> fetchDesignationDetails(List<Long> designationPoids) {
        if (designationPoids.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT DESIG_POID AS POID,
                       DESIGNATION_CODE AS CODE,
                       DESIGNATION_NAME AS DESCRIPTION
                  FROM HR_DESIGNATION_MASTER
                 WHERE DESIG_POID IN (:poids)
                 ORDER BY SEQNO
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(HrResignationConstants.PARAM_POIDS, designationPoids);
        return toLovMap(query);
    }

    private Map<Long, LovGetListDto> toLovMap(Query query) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<Long, LovGetListDto> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 3 || row[0] == null) {
                continue;
            }
            Long poid = ((Number) row[0]).longValue();
            String code = row[1] != null ? row[1].toString() : null;
            String description = row[2] != null ? row[2].toString() : null;
            map.put(poid, new LovGetListDto(poid, code, description, poid, description, null, null));
        }
        return map;
    }

    private LovGetListDto resolveResignationTypeLov(String code) {
        if (code == null || code.isBlank()) {
            return new LovGetListDto();
        }
        try {
            LovGetListDto lov = lovDataService.getLovItemByCodeFast(code.trim().toUpperCase(Locale.ROOT), "RESIGNATION_TYPE");
            return lov != null ? lov : new LovGetListDto();
        } catch (Exception ex) {
            return new LovGetListDto();
        }
    }

    private List<Long> toDistinctPositiveList(Long... poids) {
        List<Long> resolved = new ArrayList<>();
        for (Long poid : poids) {
            if (poid != null && poid > 0 && !resolved.contains(poid)) {
                resolved.add(poid);
            }
        }
        return resolved;
    }
}
