package com.asg.hr.employeemaster.service;

import com.asg.common.lib.client.ParameterServiceClient;
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
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.airsector.repository.HrAirsectorRepository;
import com.asg.hr.client.GlMasterServiceClient;
import com.asg.hr.common.repository.AdminCrMasterRepository;
import com.asg.hr.common.repository.GlobalFixedVariablesRepository;
import com.asg.hr.common.repository.GlobalShiftMasterRepository;
import com.asg.hr.departmentmaster.repository.HrDepartmentMasterRepository;
import com.asg.hr.designation.repository.HrDesignationMasterRepository;
import com.asg.hr.employeemaster.dto.*;
import com.asg.hr.employeemaster.entity.*;
import com.asg.hr.employeemaster.util.EmployeeMasterMapper;
import com.asg.hr.exceptions.CustomException;
import net.sf.jasperreports.engine.JasperReport;
import oracle.jdbc.internal.OracleTypes;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import com.asg.hr.employeemaster.enums.ActionType;
import com.asg.hr.employeemaster.repository.*;
import org.springframework.data.domain.PageRequest;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.locationmaster.repository.GlobalLocationMasterRepository;
import com.asg.hr.nationality.repository.HrNationalityRepository;
import com.asg.hr.religion.repository.ReligionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;


@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeMasterServiceImpl implements EmployeeMasterService {

    private static final String HR_EMPLOYEE_MASTER_TABLE = "HR_EMPLOYEE_MASTER";
    private static final String HR_EMPLOYEE_MASTER_POID_FIELD = "EMPLOYEE_POID";
    private static final String DET_ROW_ID = "DET_ROW_ID";
    private static final String EMPLOYEE = "Employee";
    private static final String EMPLOYEE_POID = "employeePoid";
    private static final Sort DEFAULT_EMPLOYEE_DASHBOARD_SORT = Sort.by(Sort.Order.desc(EMPLOYEE_POID));
    private static final String DOC_ID_EMPLOYEE_MASTER = "800-001";
    private static final String GL_TYPE_EMPLOYEE = "EMPLOYEE";
    private static final String PERMANENT = "PERMANENT";
    private static final String P_LOGIN_GROUP_POID = "P_LOGIN_GROUP_POID";
    private static final String P_LOGIN_COMPANY_POID = "P_LOGIN_COMPANY_POID";
    private static final String P_EMPLOYEE_POID = "P_EMPLOYEE_POID";
    private static final String P_RESULT = "P_RESULT";
    private static final String ERROR = "ERROR";
    private static final String UPDATE_LOG_STRING = "KeyId = EMPLOYEE_POID %s: DET_ROW_ID %s";
    private static final String P_REJOIN_DATE = "P_REJOIN_DATE";
    private static final String P_REJOIN_LRQ_REF = "P_REJOIN_LRQ_REF";
    private static final String P_LOGIN_USER = "P_LOGIN_USER";
    private static final String DEPENDENTS = "Dependents";

    private final HrEmployeeMasterRepository masterRepository;
    private final HrEmployeeDependentRepository dependentRepository;
    private final HrEmpDepndtsLmraDtlsRepository lmraRepository;
    private final HrEmployeeDocumentDtlRepository documentRepository;
    private final HrEmployeeExperienceDtlRepository experienceRepository;
    private final HrEmployeeLeaveHistoryRepository leaveHistoryRepository;
    private final HrAirsectorRepository airsectorRepository;
    private final HrDepartmentMasterRepository hrDepartmentMasterRepository;
    private final HrNationalityRepository hrNationalityRepository;
    private final HrDesignationMasterRepository designationRepository;
    private final ReligionRepository religionRepository;
    private final GlMasterServiceClient glMasterServiceClient;
    private final GlobalFixedVariablesRepository globalFixedVariablesRepository;
    private final GlobalShiftMasterRepository globalShiftMasterRepository;
    private final GlobalLocationMasterRepository locationMasterRepository;
    private final AdminCrMasterRepository crMasterRepository;
    private final EmployeeMasterMapper employeeMasterMapper;
    private final ParameterServiceClient parameterServiceClient;

    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listEmployees(String docId, FilterRequestDto filters, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> resolvedFilters = documentSearchService.resolveFilters(filters);

        RawSearchResult raw = documentSearchService.search(docId, resolvedFilters, operator, pageable, isDeleted, "DISPLAY_NAME", HR_EMPLOYEE_MASTER_POID_FIELD);

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeMasterResponseDto getEmployeeById(Long employeePoid) {

        HrEmployeeMaster entity = masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        return employeeMasterMapper.toResponseDto(entity);
    }

    @Override
    public EmployeeMasterResponseDto createEmployee(EmployeeMasterRequestDto requestDto) {

        validateEmployeeMasterRequest(requestDto, false, null);

        HrEmployeeMaster entity = new HrEmployeeMaster();
        employeeMasterMapper.applyHeaderFields(entity, requestDto);
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setActive("Y");
        entity.setDeleted("N");

        HrEmployeeMaster saved = masterRepository.save(entity);

        // Legacy: if EMP_GL_POID isn't set, backend creates it via PROC_GL_MASTER_CREATION after save.
        createEmployeeGlIfMissing(saved.getEmployeePoid());

        // Validate all child tables before persisting any, so logs are not written for tables that pass when a later one fails.
        validateChildTables(saved.getEmployeePoid(), requestDto);

        // Child tables: apply row-level actionType (no audit-field assignment here).
        applyDependents(saved.getEmployeePoid(), requestDto.getDependentsDetails());
        applyLmraDetails(saved.getEmployeePoid(), requestDto.getLmraDetails());
        applyExperienceDetails(saved.getEmployeePoid(), requestDto.getExperienceDetails());
        applyDocumentDetails(saved.getEmployeePoid(), requestDto.getDocumentDetails());

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getEmployeePoid().toString());
        return employeeMasterMapper.toResponseDto(saved);
    }

    @Override
    public EmployeeMasterResponseDto updateEmployee(Long employeePoid, EmployeeMasterRequestDto requestDto) {

        HrEmployeeMaster existing = masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        validateEmployeeMasterRequest(requestDto, true, employeePoid);

        HrEmployeeMaster oldEntity = new HrEmployeeMaster();
        // Copy full entity snapshot for accurate audit logging (no manual audit field assignment).
        BeanUtils.copyProperties(existing, oldEntity);

        // Apply header changes (photo is updated via separate endpoint).
        employeeMasterMapper.applyHeaderFields(existing, requestDto);

        HrEmployeeMaster saved = masterRepository.save(existing);

        // Legacy: if EMP_GL_POID isn't set, backend creates it via PROC_GL_MASTER_CREATION after save.
        createEmployeeGlIfMissing(saved.getEmployeePoid());

        // Validate all child tables before persisting any, so logs are not written for tables that pass when a later one fails.
        validateChildTables(saved.getEmployeePoid(), requestDto);

        // Child tables: apply actionType updates.
        applyDependents(saved.getEmployeePoid(), requestDto.getDependentsDetails());
        applyLmraDetails(saved.getEmployeePoid(), requestDto.getLmraDetails());
        applyExperienceDetails(saved.getEmployeePoid(), requestDto.getExperienceDetails());
        applyDocumentDetails(saved.getEmployeePoid(), requestDto.getDocumentDetails());

        loggingService.logChanges(oldEntity, saved, HrEmployeeMaster.class, UserContext.getDocumentId(), employeePoid.toString(), LogDetailsEnum.MODIFIED, HR_EMPLOYEE_MASTER_POID_FIELD);
        return employeeMasterMapper.toResponseDto(saved);
    }

    @Override
    public void deleteEmployee(Long employeePoid, DeleteReasonDto deleteReasonDto) {

        masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        documentDeleteService.deleteDocument(employeePoid, HR_EMPLOYEE_MASTER_TABLE, HR_EMPLOYEE_MASTER_POID_FIELD, deleteReasonDto, null);
    }

    @Override
    public EmployeePhotoUpdateResponseDto updateEmployeePhoto(Long employeePoid, EmployeePhotoUpdateRequestDto requestDto) {

        HrEmployeeMaster entity = masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        entity.setPhoto(requestDto.getPhoto());
        HrEmployeeMaster saved = masterRepository.save(entity);

        return EmployeePhotoUpdateResponseDto.builder()
                .employeePoid(saved.getEmployeePoid())
                .photo(saved.getPhoto())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeCountDto getEmployeeCounts() {
        return masterRepository.getEmployeeCounts();
    }

    private void validateEmployeeMasterRequest(EmployeeMasterRequestDto requestDto, boolean isUpdate, Long currentEmployeePoid) {
        validateHodReference(requestDto, isUpdate, currentEmployeePoid);
        validatePermanentServiceType(requestDto);
        validateTicketFields(requestDto);
        validateDiscontinued(requestDto);
        validateManualEmployeeCode(requestDto, isUpdate);
        validateForeignKeyReferences(requestDto);
        validateUniqueness(requestDto, isUpdate, currentEmployeePoid);
        validateGlReferences(requestDto);
    }

    private void validateHodReference(EmployeeMasterRequestDto requestDto, boolean isUpdate, Long currentEmployeePoid) {
        if (isUpdate && requestDto.getHod() != null && currentEmployeePoid != null && currentEmployeePoid.equals(requestDto.getHod())) {
            throw new ValidationException("Should not select current employee as direct supervisor");
        }
        if (requestDto.getHod() != null && !masterRepository.existsByEmployeePoid(requestDto.getHod())) {
            throw new ResourceNotFoundException(EMPLOYEE, "Direct Supervisor (HOD)", requestDto.getHod());
        }
    }

    private void validatePermanentServiceType(EmployeeMasterRequestDto requestDto) {
        if (!PERMANENT.equalsIgnoreCase(requestDto.getServiceType())) return;
        if (StringUtils.isBlank(requestDto.getCprNo())) {
            throw new ValidationException("CPR Number Is Required");
        }
        if (requestDto.getCrPoid() == null) {
            throw new ValidationException("Emp Reg Co Is Required");
        }
    }

    private void validateTicketFields(EmployeeMasterRequestDto requestDto) {
        String serviceType = requestDto.getServiceType();
        String bahNationalityPoid = getGlobalParameterValue("BAHRAIN_Nationality_Poid", "1");
        boolean isBahraini = requestDto.getNationalityPoid() != null && bahNationalityPoid != null
                && String.valueOf(requestDto.getNationalityPoid()).equalsIgnoreCase(bahNationalityPoid.trim());
        boolean anyTicketFieldPresent = requestDto.getAirSectorPoid() != null || requestDto.getTicketPeriod() != null || requestDto.getNoOfTickets() != null;
        boolean allTicketFieldsPresent = requestDto.getAirSectorPoid() != null
                && StringUtils.isNotBlank(requestDto.getTicketPeriod()) && StringUtils.isNotBlank(requestDto.getNoOfTickets());
        if (PERMANENT.equalsIgnoreCase(serviceType) && !isBahraini && !allTicketFieldsPresent) {
            throw new ValidationException("AirSector, TicketPeriod, Number of tickets are required field for all permanent expat employees...");
        }
        if ((!PERMANENT.equalsIgnoreCase(serviceType) || isBahraini) && anyTicketFieldPresent) {
            throw new ValidationException("AirSector, TicketPeriod, Number of tickets are NOT required for bahranini or remote or flexi visa employees...");
        }
    }

    private void validateDiscontinued(EmployeeMasterRequestDto requestDto) {
        if (requestDto.getDiscontinued() != null && requestDto.getDiscontinued().toUpperCase().contains("Y") && requestDto.getDiscontinuedDate() == null) {
            throw new ValidationException("Discontinued date is empty..please check");
        }
    }

    // Legacy: Employee code required on create when HR_MANUAL_EMPLOYEE_CODE=Y (company param).
    private void validateManualEmployeeCode(EmployeeMasterRequestDto requestDto, boolean isUpdate) {
        if (isUpdate) return;
        String manualEmpCode = getGlobalParameterValue("HR_MANUAL_EMPLOYEE_CODE", "");
        if ("Y".equalsIgnoreCase(StringUtils.trimToEmpty(manualEmpCode)) && StringUtils.isBlank(requestDto.getEmployeeCode())) {
            throw new ValidationException("Employee Code Is Required");
        }
    }

    private void validateForeignKeyReferences(EmployeeMasterRequestDto requestDto) {
        if (requestDto.getAirSectorPoid() != null && !airsectorRepository.existsByAirsecPoid(requestDto.getAirSectorPoid())) {
            throw new ResourceNotFoundException("Air Sector", "Air Sector Poid", requestDto.getAirSectorPoid());
        }
        if (requestDto.getDepartmentPoid() != null && !hrDepartmentMasterRepository.existsByDeptPoid(requestDto.getDepartmentPoid())) {
            throw new ResourceNotFoundException("Department", "Department Poid", requestDto.getDepartmentPoid());
        }
        if (requestDto.getNationalityPoid() != null && !hrNationalityRepository.existsByNationPoid(requestDto.getNationalityPoid())) {
            throw new ResourceNotFoundException("Nationality", "Nationality Poid", requestDto.getNationalityPoid());
        }
        if (requestDto.getReligionPoid() != null && !religionRepository.existsByReligionPoid(requestDto.getReligionPoid())) {
            throw new ResourceNotFoundException("Religion", "Religion Poid", requestDto.getReligionPoid());
        }
        if (requestDto.getDesignationPoid() != null && !designationRepository.existsByDesigPoid(requestDto.getDesignationPoid())) {
            throw new ResourceNotFoundException("Designation", "Designation Poid", requestDto.getDesignationPoid());
        }
        if (requestDto.getShiftPoid() != null && !globalShiftMasterRepository.existsByShiftPoid(requestDto.getShiftPoid())) {
            throw new ResourceNotFoundException("Shift", "Shift Poid", requestDto.getShiftPoid());
        }
        if (requestDto.getDiscontinued() != null && !globalFixedVariablesRepository.existsByVariableName(requestDto.getDiscontinued())) {
            throw new ResourceNotFoundException("Fixed Variable", "View Using", requestDto.getDiscontinued());
        }
        if (requestDto.getLocationPoid() != null && !locationMasterRepository.existsByLocationPoid(requestDto.getLocationPoid())) {
            throw new ResourceNotFoundException("Location", "Location Poid", requestDto.getLocationPoid());
        }
        if (requestDto.getCrPoid() != null && !crMasterRepository.existsByCrPoid(requestDto.getCrPoid())) {
            throw new ResourceNotFoundException("Admin Cr Master", "Cr Poid", requestDto.getCrPoid());
        }
    }

    private void validateUniqueness(EmployeeMasterRequestDto requestDto, boolean isUpdate, Long currentEmployeePoid) {
        if (requestDto.getMobile() != null) {
            boolean duplicate = isUpdate
                    ? masterRepository.existsByMobileAndEmployeePoidNot(requestDto.getMobile(), currentEmployeePoid)
                    : masterRepository.existsByMobile(requestDto.getMobile());
            if (duplicate) throw new ResourceAlreadyExistsException("Mobile", requestDto.getMobile());
        }
        if (requestDto.getFirstName() != null) {
            boolean duplicate = isUpdate
                    ? masterRepository.existsByEmployeeNameAndEmployeePoidNot(requestDto.getFirstName(), currentEmployeePoid)
                    : masterRepository.existsByEmployeeName(requestDto.getFirstName());
            if (duplicate) throw new ResourceAlreadyExistsException("Name", requestDto.getFirstName());
        }
        if (StringUtils.isNotBlank(requestDto.getEmployeeCode())) {
            String employeeCode = requestDto.getEmployeeCode().trim();
            boolean duplicate = isUpdate
                    ? masterRepository.existsByEmployeeCodeAndEmployeePoidNot(employeeCode, currentEmployeePoid)
                    : masterRepository.existsByEmployeeCode(employeeCode);
            if (duplicate) throw new ResourceAlreadyExistsException("Employee Code", employeeCode);
        }
        if (StringUtils.isNotBlank(requestDto.getCprNo())) {
            String cprNo = requestDto.getCprNo().trim();
            boolean duplicate = isUpdate
                    ? masterRepository.existsByCprNoAndEmployeePoidNot(cprNo, currentEmployeePoid)
                    : masterRepository.existsByCprNo(cprNo);
            if (duplicate) throw new ResourceAlreadyExistsException("CPR No", cprNo);
        }
        if (StringUtils.isNotBlank(requestDto.getIban())) {
            String iban = requestDto.getIban().trim();
            boolean duplicate = isUpdate
                    ? masterRepository.existsByIbanAndEmployeePoidNot(iban, currentEmployeePoid)
                    : masterRepository.existsByIban(iban);
            if (duplicate) throw new ResourceAlreadyExistsException("IBAN", iban);
        }
    }

    private void validateGlReferences(EmployeeMasterRequestDto requestDto) {
        if (requestDto.getEmpGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getEmpGlPoid());
        }
        if (requestDto.getPettyCashGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getPettyCashGlPoid());
        }
    }

    private void validateChildTables(Long employeePoid, EmployeeMasterRequestDto requestDto) {
        validateDependents(employeePoid, requestDto.getDependentsDetails());
        validateLmraDetails(employeePoid, requestDto.getLmraDetails());
        validateExperienceDetails(employeePoid, requestDto.getExperienceDetails());
    }

    private void validateDependents(Long employeePoid, List<EmployeeDependentsDtlRequestDto> dtos) {
        if (dtos == null) return;
        for (EmployeeDependentsDtlRequestDto dto : dtos) {
            if (employeePoid != null && !masterRepository.existsByEmployeePoid(employeePoid)) {
                throw new ResourceNotFoundException(EMPLOYEE, "Employee Poid", employeePoid);
            }
            if (dto.getNationality() != null && !hrNationalityRepository.existsByNationalityCode(dto.getNationality())) {
                throw new ResourceNotFoundException("Nationality", "Nationality Code", dto.getNationality());
            }
            if (dto.getActionType() == ActionType.isCreated && dto.getName() != null
                    && Boolean.TRUE.equals(dependentRepository.existsByName(dto.getName()))) {
                throw new ResourceAlreadyExistsException("Dependent Name", dto.getName());
            }
            if (dto.getActionType() == ActionType.isUpdated) {
                if (dto.getDetRowId() == null)
                    throw new IllegalArgumentException("detRowId is required for dependents isUpdated action");
                HrEmployeeDependentsDtl existing = dependentRepository.findById(new HrEmployeeDependentsDtlId(employeePoid, dto.getDetRowId()))
                        .orElseThrow(() -> new ResourceNotFoundException(DEPENDENTS, DET_ROW_ID, dto.getDetRowId()));
                if (StringUtils.isNotBlank(existing.getName()) && !existing.getName().equals(dto.getName())
                        && Boolean.TRUE.equals(dependentRepository.existsByName(dto.getName()))) {
                    throw new ResourceAlreadyExistsException("Dependent Name", dto.getName());
                }
            }
            if (dto.getActionType() == ActionType.isDeleted && dto.getDetRowId() == null) {
                throw new IllegalArgumentException("detRowId is required for dependents isDeleted action");
            }
        }
    }

    private void validateLmraDetails(Long employeePoid, List<EmployeeDepndtsLmraDtlsRequestDto> dtos) {
        if (dtos == null) return;
        for (EmployeeDepndtsLmraDtlsRequestDto dto : dtos) {
            if (employeePoid != null && !masterRepository.existsByEmployeePoid(employeePoid)) {
                throw new ResourceNotFoundException(EMPLOYEE, "Employee Poid", employeePoid);
            }
            if (dto.getActionType() == ActionType.isUpdated && dto.getDetRowId() == null) {
                throw new IllegalArgumentException("detRowId is required for lmra isUpdated action");
            }
            if (dto.getActionType() == ActionType.isDeleted && dto.getDetRowId() == null) {
                throw new IllegalArgumentException("detRowId is required for lmra isDeleted action");
            }
        }
    }

    private void validateExperienceDetails(Long employeePoid, List<EmployeeExperienceDtlRequestDto> dtos) {
        if (dtos == null) return;
        for (EmployeeExperienceDtlRequestDto dto : dtos) {
            if (employeePoid != null && !masterRepository.existsByEmployeePoid(employeePoid)) {
                throw new ResourceNotFoundException(EMPLOYEE, "Employee Poid", employeePoid);
            }
            if (dto.getActionType() == ActionType.isCreated && StringUtils.isNotBlank(dto.getEmployer())
                    && experienceRepository.existsByEmployerIgnoreCase(dto.getEmployer())) {
                throw new ResourceAlreadyExistsException("Employer", dto.getEmployer());
            }
            if (dto.getActionType() == ActionType.isUpdated) {
                if (dto.getDetRowId() == null)
                    throw new IllegalArgumentException("detRowId is required for experience isUpdated action");
                if (StringUtils.isNotBlank(dto.getEmployer()) && experienceRepository.existsByEmployerIgnoreCaseAndEmployeePoidNot(dto.getEmployer(), employeePoid)) {
                    throw new ResourceAlreadyExistsException("Employer", dto.getEmployer());
                }
            }
            if (dto.getActionType() == ActionType.isDeleted && dto.getDetRowId() == null) {
                throw new IllegalArgumentException("detRowId is required for experience isDeleted action");
            }
        }
    }

    private String getGlobalParameterValue(String parameterName, String defaultValue) {
        try {
            Optional<String> parameterResponse = parameterServiceClient.findParameterValueByName(parameterName);
            return parameterResponse
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .orElse(defaultValue);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private void createEmployeeGlIfMissing(Long employeePoid) {
        HrEmployeeMaster current = masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        if (current.getEmpGlPoid() != null) {
            return;
        }

        String empCode = current.getEmployeeCode();
        String empName = StringUtils.trimToEmpty(current.getEmployeeName());
        if (StringUtils.isNotBlank(current.getEmployeeName2())) {
            empName = (empName + " " + current.getEmployeeName2()).trim();
        }

        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_GL_MASTER_CREATION")
                    .declareParameters(
                            new SqlParameter("P_TRAN_ID", Types.NUMERIC),
                            new SqlParameter(P_LOGIN_GROUP_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_COMPANY_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_USER, Types.VARCHAR),
                            new SqlParameter("P_DOCID", Types.VARCHAR),
                            new SqlParameter("P_CODE", Types.VARCHAR),
                            new SqlParameter("P_NAME", Types.VARCHAR),
                            new SqlParameter("P_GLTYPE", Types.VARCHAR),
                            new SqlOutParameter(P_RESULT, Types.VARCHAR)
                    );

            Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource()
                    .addValue("P_TRAN_ID", employeePoid)
                    .addValue(P_LOGIN_GROUP_POID, UserContext.getGroupPoid())
                    .addValue(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid())
                    .addValue(P_LOGIN_USER, UserContext.getUserId())
                    .addValue("P_DOCID", DOC_ID_EMPLOYEE_MASTER)
                    .addValue("P_CODE", empCode)
                    .addValue("P_NAME", empName)
                    .addValue("P_GLTYPE", GL_TYPE_EMPLOYEE)
            );

            String status = (String) result.get(P_RESULT);
            if (status != null && status.toUpperCase().contains(ERROR)) {
                throw new ValidationException(status);
            }
        } catch (DataAccessException ex) {
            throw new ValidationException("PROC_GL_MASTER_CREATION failed: " + ex.getMostSpecificCause().getMessage());
        }
    }

    private void applyDependents(Long employeePoid, List<EmployeeDependentsDtlRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeDependentsDtl> existing = dependentRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeDependentsDtl::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeDependentsDtlRequestDto dto : dtos) {

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {

                long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
                nextDetRowId = Math.max(nextDetRowId, detRowId + 1);

                HrEmployeeDependentsDtl entity = new HrEmployeeDependentsDtl();
                entity.setEmployeePoid(employeePoid);
                entity.setDetRowId(detRowId);
                entity.setName(dto.getName());
                entity.setDateOfBirth(dto.getDateOfBirth());
                entity.setRelation(dto.getRelation());
                entity.setGender(dto.getGender());
                entity.setNationality(dto.getNationality());
                entity.setPassportNo(dto.getPassportNo());
                entity.setPpExpiryDate(dto.getPpExpiryDate());
                entity.setCprNo(dto.getCprNo());
                entity.setCprExpiry(dto.getCprExpiry());
                entity.setInsuDetails(dto.getInsuDetails());
                entity.setInsuStartDt(dto.getInsuStartDt());
                entity.setSponsor(dto.getSponsor());
                entity.setRpExpiry(dto.getRpExpiry());
                HrEmployeeDependentsDtl saved = dependentRepository.save(entity);
                String logDetail = String.format("Row Created on [Employee Dependent Details] with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), saved.getEmployeePoid().toString(), logDetail);
            } else if (action == ActionType.isUpdated) {
                HrEmployeeDependentsDtlId id = new HrEmployeeDependentsDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeDependentsDtl entity = dependentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DEPENDENTS, DET_ROW_ID, dto.getDetRowId()));

                HrEmployeeDependentsDtl oldDetail = new HrEmployeeDependentsDtl();
                BeanUtils.copyProperties(entity, oldDetail);

                entity.setName(dto.getName());
                entity.setDateOfBirth(dto.getDateOfBirth());
                entity.setRelation(dto.getRelation());
                entity.setGender(dto.getGender());
                entity.setNationality(dto.getNationality());
                entity.setPassportNo(dto.getPassportNo());
                entity.setPpExpiryDate(dto.getPpExpiryDate());
                entity.setCprNo(dto.getCprNo());
                entity.setCprExpiry(dto.getCprExpiry());
                entity.setInsuDetails(dto.getInsuDetails());
                entity.setInsuStartDt(dto.getInsuStartDt());
                entity.setSponsor(dto.getSponsor());
                entity.setRpExpiry(dto.getRpExpiry());
                dependentRepository.save(entity);
                String logDetail = String.format(UPDATE_LOG_STRING, entity.getEmployeePoid(), entity.getDetRowId());
                loggingService.createLog(oldDetail, entity, HrEmployeeDependentsDtl.class, UserContext.getDocumentId(), employeePoid.toString(), logDetail);
            } else if (action == ActionType.isDeleted) {
                HrEmployeeDependentsDtlId id = new HrEmployeeDependentsDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeDependentsDtl entity = dependentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DEPENDENTS, DET_ROW_ID, dto.getDetRowId()));
                dependentRepository.deleteById(id);
                loggingService.logDelete(entity, UserContext.getDocumentId(), employeePoid.toString());
            }
        }
    }

    private void applyLmraDetails(Long employeePoid, List<EmployeeDepndtsLmraDtlsRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmpDepndtsLmraDtls> existing = lmraRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmpDepndtsLmraDtls::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeDepndtsLmraDtlsRequestDto dto : dtos) {

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {
                long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
                nextDetRowId = Math.max(nextDetRowId, detRowId + 1);

                HrEmpDepndtsLmraDtls entity = new HrEmpDepndtsLmraDtls();
                entity.setEmployeePoid(employeePoid);
                entity.setDetRowId(detRowId);
                entity.setExpatCpr(dto.getExpatCpr());
                entity.setExpatPp(dto.getExpatPp());
                entity.setNationality(dto.getNationality());
                entity.setPrimaryCpr(dto.getPrimaryCpr());
                entity.setWpType(dto.getWpType());
                entity.setPermitMonths(dto.getPermitMonths());
                entity.setExpatName(dto.getExpatName());
                entity.setExpatGender(dto.getExpatGender());
                entity.setWpExpiryDate(dto.getWpExpiryDate());
                entity.setPpExpiryDate(dto.getPpExpiryDate());
                entity.setExpatCurrentStatus(dto.getExpatCurrentStatus());
                entity.setWpStatus(dto.getWpStatus());
                entity.setInOutStatus(dto.getInOutStatus());
                entity.setOffenseClassification(dto.getOffenseClassification());
                entity.setOffenceCode(dto.getOffenceCode());
                entity.setOffenceDescription(dto.getOffenceDescription());
                entity.setIntention(dto.getIntention());
                entity.setAllowMobility(dto.getAllowMobility());
                entity.setMobilityInProgress(dto.getMobilityInProgress());
                entity.setRpCancelled(dto.getRpCancelled());
                entity.setRpCancellationReason(dto.getRpCancellationReason());
                entity.setPhoto(dto.getPhoto());
                entity.setSignature(dto.getSignature());
                entity.setFingerPrint(dto.getFingerPrint());
                entity.setHealthCheckResult(dto.getHealthCheckResult());
                entity.setAdditionalBhPermit(dto.getAdditionalBhPermit());
                HrEmpDepndtsLmraDtls saved = lmraRepository.save(entity);
                String logDetail = String.format("Row Created on [Employee lmra Details] with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), saved.getEmployeePoid().toString(), logDetail);
            } else if (action == ActionType.isUpdated) {
                HrEmpDepndtsLmraDtlsId id = new HrEmpDepndtsLmraDtlsId(employeePoid, dto.getDetRowId());
                HrEmpDepndtsLmraDtls entity = lmraRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("LMRA Details", DET_ROW_ID, dto.getDetRowId()));

                HrEmpDepndtsLmraDtls oldDetail = new HrEmpDepndtsLmraDtls();
                BeanUtils.copyProperties(entity, oldDetail);

                entity.setExpatCpr(dto.getExpatCpr());
                entity.setExpatPp(dto.getExpatPp());
                entity.setNationality(dto.getNationality());
                entity.setPrimaryCpr(dto.getPrimaryCpr());
                entity.setWpType(dto.getWpType());
                entity.setPermitMonths(dto.getPermitMonths());
                entity.setExpatName(dto.getExpatName());
                entity.setExpatGender(dto.getExpatGender());
                entity.setWpExpiryDate(dto.getWpExpiryDate());
                entity.setPpExpiryDate(dto.getPpExpiryDate());
                entity.setExpatCurrentStatus(dto.getExpatCurrentStatus());
                entity.setWpStatus(dto.getWpStatus());
                entity.setInOutStatus(dto.getInOutStatus());
                entity.setOffenseClassification(dto.getOffenseClassification());
                entity.setOffenceCode(dto.getOffenceCode());
                entity.setOffenceDescription(dto.getOffenceDescription());
                entity.setIntention(dto.getIntention());
                entity.setAllowMobility(dto.getAllowMobility());
                entity.setMobilityInProgress(dto.getMobilityInProgress());
                entity.setRpCancelled(dto.getRpCancelled());
                entity.setRpCancellationReason(dto.getRpCancellationReason());
                entity.setPhoto(dto.getPhoto());
                entity.setSignature(dto.getSignature());
                entity.setFingerPrint(dto.getFingerPrint());
                entity.setHealthCheckResult(dto.getHealthCheckResult());
                entity.setAdditionalBhPermit(dto.getAdditionalBhPermit());
                lmraRepository.save(entity);
                String logDetail = String.format(UPDATE_LOG_STRING, entity.getEmployeePoid(), entity.getDetRowId());
                loggingService.createLog(oldDetail, entity, HrEmpDepndtsLmraDtls.class, UserContext.getDocumentId(), employeePoid.toString(), logDetail);
            } else if (action == ActionType.isDeleted) {
                HrEmpDepndtsLmraDtlsId id = new HrEmpDepndtsLmraDtlsId(employeePoid, dto.getDetRowId());
                HrEmpDepndtsLmraDtls entity = lmraRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("LMRA Details", DET_ROW_ID, dto.getDetRowId()));
                lmraRepository.deleteById(id);
                loggingService.logDelete(entity, UserContext.getDocumentId(), employeePoid.toString());
            }
        }
    }

    private void applyExperienceDetails(Long employeePoid, List<EmployeeExperienceDtlRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeExperienceDtl> existing = experienceRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeExperienceDtl::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeExperienceDtlRequestDto dto : dtos) {

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {

                long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
                nextDetRowId = Math.max(nextDetRowId, detRowId + 1);

                HrEmployeeExperienceDtl entity = new HrEmployeeExperienceDtl();
                entity.setEmployeePoid(employeePoid);
                entity.setDetRowId(detRowId);
                entity.setEmployer(dto.getEmployer());
                entity.setCountryLocation(dto.getCountryLocation());
                entity.setFromDate(dto.getFromDate());
                entity.setToDate(dto.getToDate());
                entity.setMonths(dto.getMonths());
                entity.setDesignation(dto.getDesignation());
                HrEmployeeExperienceDtl saved = experienceRepository.save(entity);
                String logDetail = String.format("Row Created on [Employee Experience Details] with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), saved.getEmployeePoid().toString(), logDetail);
            } else if (action == ActionType.isUpdated) {
                HrEmployeeExperienceDtlId id = new HrEmployeeExperienceDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeExperienceDtl entity = experienceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Experience", DET_ROW_ID, dto.getDetRowId()));

                HrEmployeeExperienceDtl oldDetail = new HrEmployeeExperienceDtl();
                BeanUtils.copyProperties(entity, oldDetail);

                entity.setEmployer(dto.getEmployer());
                entity.setCountryLocation(dto.getCountryLocation());
                entity.setFromDate(dto.getFromDate());
                entity.setToDate(dto.getToDate());
                entity.setMonths(dto.getMonths());
                entity.setDesignation(dto.getDesignation());
                experienceRepository.save(entity);
                String logDetail = String.format(UPDATE_LOG_STRING, entity.getEmployeePoid(), entity.getDetRowId());
                loggingService.createLog(oldDetail, entity, HrEmployeeExperienceDtl.class, UserContext.getDocumentId(), employeePoid.toString(), logDetail);
            } else if (action == ActionType.isDeleted) {
                HrEmployeeExperienceDtlId id = new HrEmployeeExperienceDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeExperienceDtl entity = experienceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Experience", DET_ROW_ID, dto.getDetRowId()));
                experienceRepository.deleteById(id);
                loggingService.logDelete(entity, UserContext.getDocumentId(), employeePoid.toString());
            }
        }
    }

    private void applyDocumentDetails(Long employeePoid, List<EmployeeDocumentDtlRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeDocumentDtl> existing = documentRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeDocumentDtl::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeDocumentDtlRequestDto dto : dtos) {

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {
                long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;
                nextDetRowId = Math.max(nextDetRowId, detRowId + 1);

                HrEmployeeDocumentDtl entity = new HrEmployeeDocumentDtl();
                entity.setEmployeePoid(employeePoid);
                entity.setDetRowId(detRowId);
                entity.setDocName(dto.getDocName());
                entity.setExpiryDate(dto.getExpiryDate());
                entity.setRemarks(dto.getRemarks());
                HrEmployeeDocumentDtl saved = documentRepository.save(entity);
                String logDetail = String.format("Row Created on [Employee Document Details] with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), saved.getEmployeePoid().toString(), logDetail);
            } else if (action == ActionType.isUpdated) {
                HrEmployeeDocumentDtlId id = new HrEmployeeDocumentDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeDocumentDtl entity = documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document", DET_ROW_ID, dto.getDetRowId()));

                HrEmployeeDocumentDtl oldDetail = new HrEmployeeDocumentDtl();
                BeanUtils.copyProperties(entity, oldDetail);

                entity.setDocName(dto.getDocName());
                entity.setExpiryDate(dto.getExpiryDate());
                entity.setRemarks(dto.getRemarks());
                documentRepository.save(entity);
                String logDetail = String.format(UPDATE_LOG_STRING, entity.getEmployeePoid(), entity.getDetRowId());
                loggingService.createLog(oldDetail, entity, HrEmployeeDocumentDtl.class, UserContext.getDocumentId(), employeePoid.toString(), logDetail);
            } else if (action == ActionType.isDeleted) {
                HrEmployeeDocumentDtlId id = new HrEmployeeDocumentDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeDocumentDtl entity = documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document", DET_ROW_ID, dto.getDetRowId()));
                documentRepository.deleteById(id);
                loggingService.logDelete(entity, UserContext.getDocumentId(), employeePoid.toString());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listEmployeeDashboardDetails(EmployeeDashboardListRequestDto request, Pageable pageable) {
        Long designationPoid = request != null ? request.getDesignationPoid() : null;
        Long locationPoid = request != null ? request.getLocationPoid() : null;
        Long departmentPoid = request != null ? request.getDepartmentPoid() : null;
        LocalDate joinDateFrom = request != null ? request.getJoinDateFrom() : null;
        LocalDate joinDateTo = request != null ? request.getJoinDateTo() : null;
        if (joinDateFrom != null && joinDateTo != null && joinDateFrom.isAfter(joinDateTo)) {
            throw new ValidationException("joinDateFrom must not be after joinDateTo");
        }

        String status = null;
        String filter = null;
        if (request != null) {
            if (StringUtils.isNotBlank(request.getStatus())) {
                status = request.getStatus().trim();
            }
            if (StringUtils.isNotBlank(request.getFilter())) {
                filter = request.getFilter().trim();
            }
        }

        Pageable safePageable = toSafeEmployeeDashboardPageable(pageable);
        Page<EmployeeDashboardDetailsDto> page = masterRepository.searchEmployeeDashboardDetails(
                designationPoid,
                locationPoid,
                departmentPoid,
                joinDateFrom,
                joinDateTo,
                status,
                filter,
                safePageable
        );
        return PaginationUtil.wrapPage(page, null);
    }

    private Pageable toSafeEmployeeDashboardPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 10, DEFAULT_EMPLOYEE_DASHBOARD_SORT);
        }
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_EMPLOYEE_DASHBOARD_SORT);
        }

        Map<String, String> sortableFieldMap = new HashMap<>();
        sortableFieldMap.put(HR_EMPLOYEE_MASTER_POID_FIELD, EMPLOYEE_POID);
        sortableFieldMap.put("EMPLOYEE_NAME", "employeeName");
        sortableFieldMap.put("EMPLOYEE_NAME2", "employeeName2");
        sortableFieldMap.put("DESIGNATION_POID", "designationPoid");
        sortableFieldMap.put("LOCATION_POID", "locationPoid");
        sortableFieldMap.put("DEPARTMENT_POID", "departmentPoid");
        sortableFieldMap.put("JOIN_DATE", "joinDate");
        sortableFieldMap.put("MOBILE", "mobile");
        sortableFieldMap.put("ACTIVE", "active");

        List<Sort.Order> safeOrders = pageable.getSort().stream()
                .map(order -> {
                    String property = order.getProperty();
                    String mapped = sortableFieldMap.get(property.toUpperCase());
                    if (mapped == null && !property.isEmpty() && Character.isLowerCase(property.charAt(0))) {
                        mapped = property;
                    }
                    if (mapped == null) {
                        mapped = EMPLOYEE_POID;
                    }
                    return new org.springframework.data.domain.Sort.Order(order.getDirection(), mapped);
                }).toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(safeOrders));
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        JasperReport mainReport = printService.load("EmployeeDetailsReportWithSalary.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public String uploadExcel(org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException("File is empty",400);
        }

        String docId = "800-001_1";
        ExcelConfig config = getExcelConfig(docId);

        jdbcTemplate.update("DELETE FROM " + config.tempTableName);

        List<List<Object>> rowsCollection = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                List<Object> colCollection = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                for (int cn = config.startColNumber - 1; cn <= config.endColNumber - 1; cn++) {
                    Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    switch (cell.getCellType()) {
                        case NUMERIC -> {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                colCollection.add(sdf.format(cell.getDateCellValue())); // returns java.util.Date
                            } else {
                                colCollection.add(cell.getNumericCellValue());
                            }
                        }
                        case STRING -> colCollection.add(cell.getStringCellValue());
                        default -> colCollection.add(null);
                    }
                }
                rowsCollection.add(colCollection);
            }
        } catch (Exception e) {
            throw new CustomException("Error processing Excel file: " + e.getMessage(), e);
        }

        saveImportedData(config.startRowNumber, rowsCollection, config.tempTableName);
        return "Successfully imported Excel data to temp table";
    }

    private ExcelConfig getExcelConfig(String docId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_GLOB_EXCEL_IMPORT_SHEETS")
                .declareParameters(
                        new SqlParameter("P_COMPANY_POID", Types.NUMERIC),
                        new SqlParameter("P_DOC_ID", Types.VARCHAR),
                        new SqlOutParameter("OUTDATA", OracleTypes.CURSOR),
                        new SqlOutParameter("P_STATUS", Types.VARCHAR)
                );

        Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource()
                .addValue("P_COMPANY_POID", UserContext.getCompanyPoid())
                .addValue("P_DOC_ID", docId)
        );

        String status = (String) result.get("P_STATUS");
        if (!"SUCCESS".equals(status)) {
            throw new CustomException("Failed to get Excel config: " + status,500);
        }

        List<Map<String, Object>> configs = (List<Map<String, Object>>) result.get("OUTDATA");
        if (configs == null || configs.isEmpty()) {
            throw new CustomException("No Excel configuration found for DOC_ID: " + docId,400);
        }

        Map<String, Object> configRow = configs.getFirst();
        ExcelConfig config = new ExcelConfig();
        config.startRowNumber = ((Number) configRow.get("START_ROW_NUMBER")).intValue();
        config.startColNumber = ((Number) configRow.get("START_COL_NUMBER")).intValue();
        config.endColNumber = ((Number) configRow.get("END_COL_NUMBER")).intValue();
        config.tempTableName = (String) configRow.get("TEMP_TABLE_NAME");
        return config;
    }

    private static class ExcelConfig {
        int startRowNumber;
        int startColNumber;
        int endColNumber;
        String tempTableName;
    }

    private void saveImportedData(int startRowNumber, List<List<Object>> rowsCollection, String tempTableName) {
        int rowNum = 0;

        for (List<Object> cols : rowsCollection) {
            rowNum++;
            if (startRowNumber <= rowNum) {
                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tempTableName + " VALUES (");
                for (Object col : cols) {
                    if (col == null) {
                        insertQuery.append("NULL,");
                    } else {
                        insertQuery.append("'").append(col.toString().replace("'", "''")).append("',");
                    }
                }
                insertQuery.setLength(insertQuery.length() - 1);
                insertQuery.append(")");

                jdbcTemplate.update(insertQuery.toString());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeLeaveDatesResponseDto getEmployeeLeaveDates(Long employeePoid) {
        masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_HR_EMP_LEAVE_DATES")
                    .declareParameters(
                            new SqlParameter(P_LOGIN_GROUP_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_COMPANY_POID, Types.NUMERIC),
                            new SqlParameter(P_EMPLOYEE_POID, Types.VARCHAR),
                            new SqlOutParameter(P_RESULT, Types.VARCHAR),
                            new SqlOutParameter("P_RESULT1", Types.VARCHAR)
                    );

            Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource()
                    .addValue(P_LOGIN_GROUP_POID, UserContext.getGroupPoid())
                    .addValue(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid())
                    .addValue(P_EMPLOYEE_POID, String.valueOf(employeePoid))
            );

            return EmployeeLeaveDatesResponseDto.builder()
                    .startDate((String) result.get(P_RESULT))
                    .periodEndDate((String) result.get("P_RESULT1"))
                    .build();
        } catch (DataAccessException ex) {
            throw new ValidationException("PROC_HR_EMP_LEAVE_DATES failed: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public String updateLeaveRejoin(Long employeePoid, LeaveRejoinUpdateRequestDto request) {
        masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_HR_EMP_REJOINDT_UPDATE_V2")
                    .declareParameters(
                            new SqlParameter(P_LOGIN_GROUP_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_COMPANY_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_USER, Types.VARCHAR),
                            new SqlParameter(P_EMPLOYEE_POID, Types.VARCHAR),
                            new SqlParameter(P_REJOIN_DATE, Types.DATE),
                            new SqlParameter(P_REJOIN_LRQ_REF, Types.VARCHAR),
                            new SqlOutParameter(P_RESULT, Types.VARCHAR)
                    );

            Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource()
                    .addValue(P_LOGIN_GROUP_POID, UserContext.getGroupPoid())
                    .addValue(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid())
                    .addValue(P_LOGIN_USER, UserContext.getUserId())
                    .addValue(P_EMPLOYEE_POID, String.valueOf(employeePoid))
                    .addValue(P_REJOIN_DATE, java.sql.Date.valueOf(request.getRejoinDate()))
                    .addValue(P_REJOIN_LRQ_REF, request.getRejoinLrqRef())
            );

            String status = (String) result.get(P_RESULT);
            if (status != null && status.contains(ERROR)) {
                throw new ValidationException(status);
            }
            return status;
        } catch (ValidationException e) {
            throw e;
        } catch (DataAccessException ex) {
            throw new ValidationException("PROC_HR_EMP_REJOINDT_UPDATE_V2 failed: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public String removeLeaveRejoin(Long employeePoid, LeaveRejoinRemoveRequestDto request) {
        masterRepository.findByEmployeePoid(employeePoid).orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_HR_EMP_REJOINDT_REMOVE")
                    .declareParameters(
                            new SqlParameter(P_LOGIN_GROUP_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_COMPANY_POID, Types.NUMERIC),
                            new SqlParameter(P_LOGIN_USER, Types.VARCHAR),
                            new SqlParameter(P_EMPLOYEE_POID, Types.VARCHAR),
                            new SqlParameter(P_REJOIN_DATE, Types.DATE),
                            new SqlParameter(P_REJOIN_LRQ_REF, Types.VARCHAR),
                            new SqlOutParameter(P_RESULT, Types.VARCHAR)
                    );

            java.sql.Date rejoinSqlDate = request.getRejoinDate() != null ? java.sql.Date.valueOf(request.getRejoinDate()) : null;

            Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource()
                    .addValue(P_LOGIN_GROUP_POID, UserContext.getGroupPoid())
                    .addValue(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid())
                    .addValue(P_LOGIN_USER, UserContext.getUserId())
                    .addValue(P_EMPLOYEE_POID, String.valueOf(employeePoid))
                    .addValue(P_REJOIN_DATE, rejoinSqlDate)
                    .addValue(P_REJOIN_LRQ_REF, request.getRejoinLrqRef())
            );

            String status = (String) result.get(P_RESULT);
            if (status != null && status.contains(ERROR)) {
                throw new ValidationException(status);
            }
            return status;
        } catch (ValidationException e) {
            throw e;
        } catch (DataAccessException ex) {
            throw new ValidationException("PROC_HR_EMP_REJOINDT_REMOVE failed: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public LmraUploadResponse uploadLmraData() {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_HR_EMP_UPLOAD_LMRA_DATA")
                .declareParameters(
                        new SqlParameter(P_EMPLOYEE_POID, Types.VARCHAR),
                        new SqlParameter(P_LOGIN_GROUP_POID, Types.NUMERIC),
                        new SqlParameter(P_LOGIN_COMPANY_POID, Types.NUMERIC),
                        new SqlParameter(P_LOGIN_USER, Types.VARCHAR),
                        new SqlOutParameter(P_RESULT, Types.VARCHAR)
                );

        Map<String, Object> result = jdbcCall.execute(new MapSqlParameterSource()
                .addValue(P_EMPLOYEE_POID, null)
                .addValue(P_LOGIN_GROUP_POID, UserContext.getGroupPoid())
                .addValue(P_LOGIN_COMPANY_POID, UserContext.getCompanyPoid())
                .addValue(P_LOGIN_USER, UserContext.getUserId())
        );

        String status = (String) result.get(P_RESULT);

        if (status != null && status.contains(ERROR)) {
            throw new ValidationException(status);
        }

        List<EmployeeDepndtsLmraDtlsResponseDto> lmraDetails =
                jdbcTemplate.query(
                        "SELECT * FROM HR_EMP_DEPNDTS_LMRA_DTLS ORDER BY DET_ROW_ID DESC",
                        (rs, rowNum) -> EmployeeDepndtsLmraDtlsResponseDto.builder()
                                .employeePoid(rs.getLong(HR_EMPLOYEE_MASTER_POID_FIELD))
                                .detRowId(rs.getLong(DET_ROW_ID))
                                .expatCpr(rs.getString("EXPAT_CPR"))
                                .expatPp(rs.getString("EXPAT_PP"))
                                .nationality(rs.getString("NATIONALITY"))
                                .primaryCpr(rs.getString("PRIMARY_CPR"))
                                .wpType(rs.getString("WP_TYPE"))
                                .permitMonths(rs.getObject("PERMIT_MONTHS") != null ? rs.getInt("PERMIT_MONTHS") : null)
                                .expatName(rs.getString("EXPAT_NAME"))
                                .expatGender(rs.getString("EXPAT_GENDER"))
                                .wpExpiryDate(rs.getObject("WP_EXPIRY_DATE", LocalDate.class))
                                .ppExpiryDate(rs.getObject("PP_EXPIRY_DATE", LocalDate.class))
                                .expatCurrentStatus(rs.getString("EXPAT_CURRENT_STATUS"))
                                .wpStatus(rs.getString("WP_STATUS"))
                                .inOutStatus(rs.getString("IN_OUT_STATUS"))
                                .offenseClassification(rs.getString("OFFENSE_CLASSIFICATION"))
                                .offenceCode(rs.getString("OFFENCE_CODE"))
                                .offenceDescription(rs.getString("OFFENCE_DESCRIPTION"))
                                .intention(rs.getString("INTENTION"))
                                .allowMobility(rs.getString("ALLOW_MOBILITY"))
                                .mobilityInProgress(rs.getString("MOBILITY_IN_PROGRESS"))
                                .rpCancelled(rs.getString("RP_CANCELLED"))
                                .rpCancellationReason(rs.getString("RP_CANCELLATION_REASON"))
                                .photo(rs.getString("PHOTO"))
                                .signature(rs.getString("SIGNATURE"))
                                .fingerPrint(rs.getString("FINGER_PRINT"))
                                .healthCheckResult(rs.getString("HEALTH_CHECK_RESULT"))
                                .additionalBhPermit(rs.getString("ADDITIONAL_BH_PERMIT"))
                                .build()
                );

        return LmraUploadResponse.builder()
                .status(status)
                .lmraDetails(lmraDetails)
                .build();
    }
}

