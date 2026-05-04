package com.asg.hr.leaverejoin.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.departmentmaster.entity.HrDepartmentMaster;
import com.asg.hr.departmentmaster.repository.HrDepartmentMasterRepository;
import com.asg.hr.designation.entity.HrDesignationMaster;
import com.asg.hr.designation.repository.DesignationRepository;
import com.asg.hr.employeemaster.entity.HrEmployeeLeaveHistory;
import com.asg.hr.employeemaster.entity.HrEmployeeMaster;
import com.asg.hr.employeemaster.repository.HrEmployeeLeaveHistoryRepository;
import com.asg.hr.employeemaster.repository.HrEmployeeMasterRepository;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinRequest;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinResponse;
import com.asg.hr.leaverejoin.entity.HrEmployeeRejoinHdr;
import com.asg.hr.leaverejoin.repository.EmployeeLeaveRejoinProcRepository;
import com.asg.hr.leaverejoin.repository.HrEmployeeRejoinRepository;
import com.asg.hr.leaverejoin.service.EmployeeLeaveRejoinService;
import com.asg.hr.leaverejoin.util.EmployeeLeaveRejoinConstants;
import com.asg.hr.leaverejoin.util.EmployeeLeaveRejoinMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeLeaveRejoinServiceImpl implements EmployeeLeaveRejoinService {

    private final HrEmployeeRejoinRepository repository;
    private final EmployeeLeaveRejoinProcRepository procRepository;
    private final HrEmployeeMasterRepository employeeRepository;
    private final HrEmployeeLeaveHistoryRepository leaveHistoryRepository;
    private final HrDepartmentMasterRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final LovDataService lovDataService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final EmployeeLeaveRejoinMapper mapper;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        List<FilterDto> resolvedFilters = new ArrayList<>(
                documentSearchService.resolveDateFilters(
                        filters,
                        EmployeeLeaveRejoinConstants.TRANSACTION_DATE_FIELD,
                        startDate,
                        endDate
                )
        );

        if (!canAccessAllEmployees()) {
            Long currentEmployeePoid = resolveCurrentEmployeePoid();
            resolvedFilters.add(new FilterDto(
                    EmployeeLeaveRejoinConstants.EMPLOYEE_POID_FIELD,
                    String.valueOf(currentEmployeePoid != null ? currentEmployeePoid : -1L)
            ));
        }

        RawSearchResult raw = documentSearchService.search(
                getRequiredDocumentId(),
                resolvedFilters,
                documentSearchService.resolveOperator(filters),
                pageable,
                documentSearchService.resolveIsDeleted(filters),
                EmployeeLeaveRejoinConstants.DOC_REF_FIELD,
                EmployeeLeaveRejoinConstants.KEY_FIELD
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeLeaveRejoinResponse getById(Long transactionPoid) {
        HrEmployeeRejoinHdr entity = getAccessibleEntity(transactionPoid);
        EmployeeLeaveRejoinResponse response = mapper.toResponse(entity);
        enrichResponse(response);
        return response;
    }

    @Override
    @Transactional
    public EmployeeLeaveRejoinResponse create(EmployeeLeaveRejoinRequest request) {
        validateContext();
        assertEmployeeAccess(request.getEmployeePoid());
        validatePassportReceivedValue(request.getPassportReceived());

        HrEmployeeMaster employee = validateEmployee(request.getEmployeePoid());
        EmployeeLeaveRejoinEmployeeDetailsResponse employeeDetails = resolveEmployeeDetails(request.getEmployeePoid(), employee);
        EmployeeLeaveRejoinLeaveDetailsResponse leaveDetails = requireLeaveDetails(request.getEmployeePoid(), request.getLeaveRequestPoid());
        validateRejoinDate(request.getDateOfRejoining(), leaveDetails.getDateProceededOnLeave());
        validateDuplicateLeaveRequest(request.getEmployeePoid(), request.getLeaveRequestPoid(), null);

        HrEmployeeRejoinHdr entity = mapper.toEntity(request);
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now());

        HrEmployeeRejoinHdr saved = repository.save(entity);
        entityManager.flush();
        entityManager.refresh(saved);
        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                saved.getTransactionPoid().toString(),
                String.format("%s %s", LogDetailsEnum.CREATED, saved.getDocRef())
        );

        EmployeeLeaveRejoinResponse response = mapper.toResponse(saved);
        enrichResponse(response, employeeDetails, leaveDetails);
        return response;
    }

    @Override
    @Transactional
    public EmployeeLeaveRejoinResponse update(Long transactionPoid, EmployeeLeaveRejoinRequest request) {
        validateContext();
        HrEmployeeRejoinHdr entity = getAccessibleEntity(transactionPoid);
        assertEmployeeAccess(request.getEmployeePoid());
        validatePassportReceivedValue(request.getPassportReceived());

        HrEmployeeMaster employee = validateEmployee(request.getEmployeePoid());
        EmployeeLeaveRejoinEmployeeDetailsResponse employeeDetails = resolveEmployeeDetails(request.getEmployeePoid(), employee);
        EmployeeLeaveRejoinLeaveDetailsResponse leaveDetails = requireLeaveDetails(request.getEmployeePoid(), request.getLeaveRequestPoid());
        validateRejoinDate(request.getDateOfRejoining(), leaveDetails.getDateProceededOnLeave());
        validateDuplicateLeaveRequest(request.getEmployeePoid(), request.getLeaveRequestPoid(), transactionPoid);

        HrEmployeeRejoinHdr oldEntity = new HrEmployeeRejoinHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        mapper.updateEntity(entity, request);
        entity.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : entity.getTransactionDate());

        HrEmployeeRejoinHdr saved = repository.save(entity);
        loggingService.logChanges(
                oldEntity,
                saved,
                HrEmployeeRejoinHdr.class,
                getRequiredDocumentId(),
                transactionPoid.toString(),
                LogDetailsEnum.MODIFIED,
                EmployeeLeaveRejoinConstants.KEY_FIELD
        );

        EmployeeLeaveRejoinResponse response = mapper.toResponse(saved);
        enrichResponse(response, employeeDetails, leaveDetails);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        getAccessibleEntity(transactionPoid);
        documentDeleteService.deleteDocument(
                transactionPoid,
                EmployeeLeaveRejoinConstants.TABLE_NAME,
                EmployeeLeaveRejoinConstants.KEY_FIELD,
                deleteReasonDto,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeLeaveRejoinEmployeeDetailsResponse getEmployeeDetails(Long employeePoid) {
        assertEmployeeAccess(employeePoid);
        HrEmployeeMaster employee = validateEmployee(employeePoid);
        EmployeeLeaveRejoinEmployeeDetailsResponse response = resolveEmployeeDetails(employeePoid, employee);
        response.setEmployeeDet(resolveLovByPoid(response.getEmployeePoid(), EmployeeLeaveRejoinConstants.EMPLOYEE_LOV));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeLeaveRejoinLeaveDetailsResponse getLeaveDetails(Long employeePoid, Long leaveRequestPoid) {
        assertEmployeeAccess(employeePoid);
        validateEmployee(employeePoid);
        EmployeeLeaveRejoinLeaveDetailsResponse response = requireLeaveDetails(employeePoid, leaveRequestPoid);
        response.setEmployeeDet(resolveLovByPoid(response.getEmployeePoid(), EmployeeLeaveRejoinConstants.EMPLOYEE_LOV));
        response.setLeaveRequestDet(resolveLovByPoid(response.getLeaveRequestPoid(), EmployeeLeaveRejoinConstants.LEAVE_REQUEST_LOV));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] print(Long transactionPoid) throws JRException {
        getAccessibleEntity(transactionPoid);
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, getRequiredDocumentId());
        JasperReport mainReport = printService.load("HR/EmployeeRejoiningReport.jrxml");
        try {
            return printService.fillReportToPdf(mainReport, params, dataSource);
        } catch (JRException e) {
            throw e;
        } catch (Exception e) {
            throw new JRException(e);
        }
    }

    private void validateContext() {
        if (UserContext.getCompanyPoid() == null) {
            throw new ValidationException("Company poid is mandatory");
        }
        getRequiredDocumentId();
    }

    private String getRequiredDocumentId() {
        String docId = UserContext.getDocumentId();
        if (docId == null || docId.isBlank()) {
            throw new ValidationException("Document id is mandatory");
        }
        return docId;
    }

    private HrEmployeeRejoinHdr getAccessibleEntity(Long transactionPoid) {
        HrEmployeeRejoinHdr entity = repository.findByTransactionPoidAndDeletedNot(
                        transactionPoid,
                        EmployeeLeaveRejoinConstants.DELETED_YES
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        EmployeeLeaveRejoinConstants.RESOURCE_NAME,
                        EmployeeLeaveRejoinConstants.KEY_FIELD,
                        transactionPoid
                ));

        assertEmployeeAccess(entity.getEmployeePoid());
        return entity;
    }

    private HrEmployeeMaster validateEmployee(Long employeePoid) {
        HrEmployeeMaster employee = employeeRepository.findByEmployeePoid(employeePoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        EmployeeLeaveRejoinConstants.EMPLOYEE_RESOURCE_NAME,
                        EmployeeLeaveRejoinConstants.EMPLOYEE_POID_FIELD,
                        employeePoid
                ));

        if (!EmployeeLeaveRejoinConstants.ACTIVE_YES.equalsIgnoreCase(trim(employee.getActive()))) {
            throw new ValidationException("Employee is not active.");
        }
        return employee;
    }

    private EmployeeLeaveRejoinEmployeeDetailsResponse resolveEmployeeDetails(Long employeePoid, HrEmployeeMaster employee) {
        EmployeeLeaveRejoinEmployeeDetailsResponse response = procRepository.getEmployeeDetails(employeePoid);
        if (response != null && isSuccess(response.getStatus())
                && hasText(response.getDepartmentName())
                && hasText(response.getDesignationName())) {
            response.setEmployeePoid(employeePoid);
            return response;
        }

        String departmentName = employee.getDepartmentPoid() != null
                ? departmentRepository.findById(employee.getDepartmentPoid()).map(HrDepartmentMaster::getDeptName).orElse(null)
                : null;
        String designationName = employee.getDesignationPoid() != null
                ? designationRepository.findById(employee.getDesignationPoid()).map(HrDesignationMaster::getDesignationName).orElse(null)
                : null;

        if (!hasText(departmentName) || !hasText(designationName)) {
            throw new ValidationException("Employee master data must have active department and designation.");
        }

        return EmployeeLeaveRejoinEmployeeDetailsResponse.builder()
                .employeePoid(employeePoid)
                .departmentName(departmentName)
                .designationName(designationName)
                .status(EmployeeLeaveRejoinConstants.STATUS_SUCCESS)
                .build();
    }

    private EmployeeLeaveRejoinLeaveDetailsResponse requireLeaveDetails(Long employeePoid, Long leaveRequestPoid) {
        EmployeeLeaveRejoinLeaveDetailsResponse response = procRepository.getLeaveDetails(employeePoid, leaveRequestPoid);
        if (response != null && isSuccess(response.getStatus()) && response.getDateProceededOnLeave() != null) {
            validateLeaveRequestOwnership(employeePoid, response.getEmployeePoid());
            return response;
        }

        Optional<HrEmployeeLeaveHistory> leaveHistory = leaveHistoryRepository
                .findTopByEmployeePoidAndSourceDocPoidAndDeletedNotOrderByLeaveHistPoidDescDetRowIdDesc(
                        employeePoid,
                        leaveRequestPoid,
                        EmployeeLeaveRejoinConstants.DELETED_YES
                );

        if (leaveHistory.isPresent() && leaveHistory.get().getLeaveStartDate() != null) {
            return EmployeeLeaveRejoinLeaveDetailsResponse.builder()
                    .employeePoid(employeePoid)
                    .leaveRequestPoid(leaveRequestPoid)
                    .dateProceededOnLeave(leaveHistory.get().getLeaveStartDate())
                    .plannedRejoinDate(null)
                    .status(EmployeeLeaveRejoinConstants.STATUS_SUCCESS)
                    .build();
        }

        throw new ValidationException("Selected leave request details were not found.");
    }

    private void validateRejoinDate(LocalDate rejoinDate, LocalDate leaveStartDate) {
        if (leaveStartDate != null && rejoinDate != null && rejoinDate.isBefore(leaveStartDate)) {
            throw new ValidationException("Rejoin date cannot be before leave start date.");
        }
    }

    private void validatePassportReceivedValue(String passportReceived) {
        String normalized = normalizePassportReceived(passportReceived);
        if (normalized == null) {
            return;
        }

        if (!"YES".equals(normalized) && !"NO".equals(normalized)) {
            throw new ValidationException("Passport received must be YES or NO.");
        }
    }

    private void validateDuplicateLeaveRequest(Long employeePoid, Long leaveRequestPoid, Long transactionPoid) {
        boolean exists = transactionPoid == null
                ? repository.existsByEmployeePoidAndLeaveRequestPoidAndDeletedNot(
                employeePoid,
                leaveRequestPoid,
                EmployeeLeaveRejoinConstants.DELETED_YES
        )
                : repository.existsByEmployeePoidAndLeaveRequestPoidAndDeletedNotAndTransactionPoidNot(
                employeePoid,
                leaveRequestPoid,
                EmployeeLeaveRejoinConstants.DELETED_YES,
                transactionPoid
        );

        if (exists) {
            throw new ValidationException("Selected leave request already has an employee rejoining record.");
        }
    }

    private void validateLeaveRequestOwnership(Long expectedEmployeePoid, Long leaveEmployeePoid) {
        if (expectedEmployeePoid != null && leaveEmployeePoid != null && !expectedEmployeePoid.equals(leaveEmployeePoid)) {
            throw new ValidationException("Employee on Leave Request and Rejoining Form should be same.");
        }
    }

    private void enrichResponse(EmployeeLeaveRejoinResponse response) {
        if (response == null) {
            return;
        }

        EmployeeLeaveRejoinEmployeeDetailsResponse employeeDetails = null;
        HrEmployeeMaster employee = response.getEmployeePoid() != null
                ? employeeRepository.findByEmployeePoid(response.getEmployeePoid()).orElse(null)
                : null;
        if (employee != null) {
            employeeDetails = resolveEmployeeDetails(response.getEmployeePoid(), employee);
        }

        EmployeeLeaveRejoinLeaveDetailsResponse leaveDetails = null;
        if (response.getEmployeePoid() != null && response.getLeaveRequestPoid() != null) {
            try {
                leaveDetails = requireLeaveDetails(response.getEmployeePoid(), response.getLeaveRequestPoid());
            } catch (ValidationException ex) {
                log.warn("Unable to enrich leave details for transactionPoid={}", response.getTransactionPoid(), ex);
            }
        }

        enrichResponse(response, employeeDetails, leaveDetails);
    }

    private void enrichResponse(
            EmployeeLeaveRejoinResponse response,
            EmployeeLeaveRejoinEmployeeDetailsResponse employeeDetails,
            EmployeeLeaveRejoinLeaveDetailsResponse leaveDetails
    ) {
        if (response == null) {
            return;
        }

        if (employeeDetails != null) {
            response.setDepartmentName(employeeDetails.getDepartmentName());
            response.setDesignationName(employeeDetails.getDesignationName());
        }

        if (leaveDetails != null) {
            response.setDateProceededOnLeave(leaveDetails.getDateProceededOnLeave());
            response.setPlannedRejoinDate(leaveDetails.getPlannedRejoinDate());
        }

        response.setEmployeeDet(resolveLovByPoid(response.getEmployeePoid(), EmployeeLeaveRejoinConstants.EMPLOYEE_LOV));
        response.setLeaveRequestDet(resolveLovByPoid(response.getLeaveRequestPoid(), EmployeeLeaveRejoinConstants.LEAVE_REQUEST_LOV));
        response.setPassportReceivedDet(resolveLovByCode(response.getPassportReceived(), EmployeeLeaveRejoinConstants.PASSPORT_RECEIVED_LOV));
    }

    private LovGetListDto resolveLovByPoid(Long poid, String lovName) {
        if (poid == null) {
            return new LovGetListDto();
        }
        try {
            LovGetListDto lov = lovDataService.getDetailsByPoidAndLovNameFast(poid, lovName);
            return lov != null ? lov : new LovGetListDto();
        } catch (Exception ex) {
            log.warn("Unable to resolve LOV {} for poid {}", lovName, poid, ex);
            return new LovGetListDto();
        }
    }

    private LovGetListDto resolveLovByCode(String code, String lovName) {
        String normalizedCode = normalizePassportReceived(code);
        if (!hasText(normalizedCode)) {
            return new LovGetListDto();
        }
        try {
            LovGetListDto lov = lovDataService.getLovItemByCodeFast(normalizedCode, lovName);
            return lov != null ? lov : new LovGetListDto();
        } catch (Exception ex) {
            log.warn("Unable to resolve LOV {} for code {}", lovName, code, ex);
            return new LovGetListDto();
        }
    }

    private void assertEmployeeAccess(Long employeePoid) {
        if (employeePoid == null || canAccessAllEmployees()) {
            return;
        }

        Long currentEmployeePoid = resolveCurrentEmployeePoid();
        if (currentEmployeePoid == null || !currentEmployeePoid.equals(employeePoid)) {
            throw new ValidationException("You are not allowed to access another employee record.");
        }
    }

    private Long resolveCurrentEmployeePoid() {
        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            return null;
        }

        return employeeRepository.findByLoginUserPoid(userPoid)
                .map(HrEmployeeMaster::getEmployeePoid)
                .orElse(null);
    }

    private boolean canAccessAllEmployees() {
        String userRole = UserContext.getUserRole();
        if (!hasText(userRole)) {
            return false;
        }

        String normalizedRole = userRole.trim().toUpperCase(Locale.ROOT);
        return normalizedRole.contains("ADMIN") || normalizedRole.contains("HR");
    }

    private boolean isSuccess(String status) {
        return hasText(status) && !status.toUpperCase(Locale.ROOT).contains(EmployeeLeaveRejoinConstants.STATUS_ERROR);
    }

    private boolean hasText(String value) {
        return trim(value) != null;
    }

    private String normalizePassportReceived(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return null;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("Y".equals(upper)) {
            return "YES";
        }
        if ("N".equals(upper)) {
            return "NO";
        }
        return upper;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
