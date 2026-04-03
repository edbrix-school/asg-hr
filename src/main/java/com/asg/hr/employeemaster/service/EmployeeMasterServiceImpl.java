package com.asg.hr.employeemaster.service;

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
import net.sf.jasperreports.engine.JasperReport;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeMasterServiceImpl implements EmployeeMasterService {

    private static final String HR_EMPLOYEE_MASTER_TABLE = "HR_EMPLOYEE_MASTER";
    private static final String HR_EMPLOYEE_MASTER_POID_FIELD = "EMPLOYEE_POID";
    private static final String DET_ROW_ID = "DET_ROW_ID";
    private static final String EMPLOYEE = "Employee";
    private static final Sort DEFAULT_EMPLOYEE_DASHBOARD_SORT = Sort.by(Sort.Order.desc("employeePoid"));

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

    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final PrintService printService;
    private final DataSource dataSource;

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

        HrEmployeeMaster entity = masterRepository.findByEmployeePoid(employeePoid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

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

        HrEmployeeMaster existing = masterRepository.findByEmployeePoid(employeePoid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        validateEmployeeMasterRequest(requestDto, true, employeePoid);

        HrEmployeeMaster oldEntity = new HrEmployeeMaster();
        // Copy full entity snapshot for accurate audit logging (no manual audit field assignment).
        BeanUtils.copyProperties(existing, oldEntity);

        // Apply header changes (photo is updated via separate endpoint).
        employeeMasterMapper.applyHeaderFields(existing, requestDto);

        HrEmployeeMaster saved = masterRepository.save(existing);

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

        masterRepository.findByEmployeePoid(employeePoid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        documentDeleteService.deleteDocument(employeePoid, HR_EMPLOYEE_MASTER_TABLE, HR_EMPLOYEE_MASTER_POID_FIELD, deleteReasonDto, null);
    }

    @Override
    public EmployeePhotoUpdateResponseDto updateEmployeePhoto(Long employeePoid, EmployeePhotoUpdateRequestDto requestDto) {

        HrEmployeeMaster entity = masterRepository.findByEmployeePoid(employeePoid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

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
        if (isUpdate && requestDto.getHod() != null && currentEmployeePoid != null && currentEmployeePoid.equals(requestDto.getHod())) {
            throw new ValidationException("Should not select current employee as direct supervisor");
        }

        if (requestDto.getAirSectorPoid() != null && !airsectorRepository.existsByAirsecPoid(requestDto.getAirSectorPoid())) {
            throw new ResourceNotFoundException("Air Sector", "Air Sector Poid", requestDto.getAirSectorPoid());
        }
        if (requestDto.getDepartmentPoid() != null && !hrDepartmentMasterRepository.existsByDeptPoid(requestDto.getDepartmentPoid())) {
            throw new ResourceNotFoundException("Department", "Department Poid", requestDto.getDepartmentPoid());
        }
        if (requestDto.getNationalityPoid() != null && Boolean.FALSE.equals(hrNationalityRepository.existsByNationPoid(requestDto.getNationalityPoid()))) {
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

        if (isUpdate && requestDto.getMobile() != null && masterRepository.existsByMobileAndEmployeePoidNot(requestDto.getMobile(), currentEmployeePoid)) {
            throw new ResourceAlreadyExistsException("Mobile", requestDto.getMobile());
        }
        if (!isUpdate && requestDto.getMobile() != null && masterRepository.existsByMobile(requestDto.getMobile())) {
            throw new ResourceAlreadyExistsException("Mobile", requestDto.getMobile());
        }
        if (isUpdate && requestDto.getFirstName() != null && masterRepository.existsByEmployeeNameAndEmployeePoidNot(requestDto.getFirstName(), currentEmployeePoid)) {
            throw new ResourceAlreadyExistsException("Name", requestDto.getFirstName());
        }
        if (!isUpdate && requestDto.getFirstName() != null && masterRepository.existsByEmployeeName(requestDto.getFirstName())) {
            throw new ResourceAlreadyExistsException("Name", requestDto.getFirstName());
        }

        if (requestDto.getEmpGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getEmpGlPoid());
        }
        if (requestDto.getPettyCashGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getPettyCashGlPoid());
        }
    }

    private void applyDependents(Long employeePoid, List<EmployeeDependentsDtlRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeDependentsDtl> existing = dependentRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeDependentsDtl::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeDependentsDtlRequestDto dto : dtos) {

            if (dto.getEmployeePoid() != null) {
                if (!masterRepository.existsByEmployeePoid(dto.getEmployeePoid())) {
                    throw new ResourceNotFoundException("Employee", "Employee Poid", dto.getEmployeePoid());
                }
            }
            if (dto.getNationality() != null) {
                if (Boolean.FALSE.equals(hrNationalityRepository.existsByNationalityCode(dto.getNationality()))) {
                    throw new ResourceNotFoundException("Nationality", "Nationality Code", dto.getNationality());
                }
            }

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {

                if (dto.getName() != null) {
                    if (Boolean.TRUE.equals(dependentRepository.existsByName(dto.getName()))) {
                        throw new ResourceAlreadyExistsException("Dependent Name", dto.getName());
                    }
                }

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
                dependentRepository.save(entity);
            } else if (action == ActionType.isUpdated) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for dependents isUpdated action");
                }

                HrEmployeeDependentsDtlId id = new HrEmployeeDependentsDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeDependentsDtl entity = dependentRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Dependents", DET_ROW_ID, dto.getDetRowId()));

                if (entity != null && StringUtils.isNotBlank(entity.getName()) && !entity.getName().equals(dto.getName())) {
                    if (Boolean.TRUE.equals(dependentRepository.existsByName(dto.getName()))) {
                        throw new ResourceAlreadyExistsException("Dependent Name", dto.getName());
                    }
                }

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
            } else if (action == ActionType.isDeleted) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for dependents isDeleted action");
                }
                HrEmployeeDependentsDtlId id = new HrEmployeeDependentsDtlId(employeePoid, dto.getDetRowId());
                dependentRepository.deleteById(id);
            }
        }
    }

    private void applyLmraDetails(Long employeePoid, List<EmployeeDepndtsLmraDtlsRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmpDepndtsLmraDtls> existing = lmraRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmpDepndtsLmraDtls::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeDepndtsLmraDtlsRequestDto dto : dtos) {

            if (dto.getEmployeePoid() != null) {
                if (!masterRepository.existsByEmployeePoid(dto.getEmployeePoid())) {
                    throw new ResourceNotFoundException("Employee", "Employee Poid", dto.getEmployeePoid());
                }
            }

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
                lmraRepository.save(entity);
            } else if (action == ActionType.isUpdated) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for lmra isUpdated action");
                }

                HrEmpDepndtsLmraDtlsId id = new HrEmpDepndtsLmraDtlsId(employeePoid, dto.getDetRowId());
                HrEmpDepndtsLmraDtls entity = lmraRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("LMRA Details", DET_ROW_ID, dto.getDetRowId()));

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
            } else if (action == ActionType.isDeleted) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for lmra isDeleted action");
                }
                HrEmpDepndtsLmraDtlsId id = new HrEmpDepndtsLmraDtlsId(employeePoid, dto.getDetRowId());
                lmraRepository.deleteById(id);
            }
        }
    }

    private void applyExperienceDetails(Long employeePoid, List<EmployeeExperienceDtlRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeExperienceDtl> existing = experienceRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeExperienceDtl::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeExperienceDtlRequestDto dto : dtos) {

            if (dto.getEmployeePoid() != null) {
                if (!masterRepository.existsByEmployeePoid(dto.getEmployeePoid())) {
                    throw new ResourceNotFoundException("Employee", "Employee Poid", dto.getEmployeePoid());
                }
            }

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {

                if (StringUtils.isNotBlank(dto.getEmployer())) {
                    if (experienceRepository.existsByEmployerIgnoreCase(dto.getEmployer())) {
                        throw new ResourceAlreadyExistsException("Employer", dto.getEmployer());
                    }
                }

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
                experienceRepository.save(entity);
            } else if (action == ActionType.isUpdated) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for experience isUpdated action");
                }

                if (StringUtils.isNotBlank(dto.getEmployer())) {
                    if (experienceRepository.existsByEmployerIgnoreCaseAndEmployeePoidNot(dto.getEmployer(), employeePoid)) {
                        throw new ResourceAlreadyExistsException("Employer", dto.getEmployer());
                    }
                }

                HrEmployeeExperienceDtlId id = new HrEmployeeExperienceDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeExperienceDtl entity = experienceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Experience", DET_ROW_ID, dto.getDetRowId()));

                entity.setEmployer(dto.getEmployer());
                entity.setCountryLocation(dto.getCountryLocation());
                entity.setFromDate(dto.getFromDate());
                entity.setToDate(dto.getToDate());
                entity.setMonths(dto.getMonths());
                entity.setDesignation(dto.getDesignation());
                experienceRepository.save(entity);
            } else if (action == ActionType.isDeleted) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for experience isDeleted action");
                }
                HrEmployeeExperienceDtlId id = new HrEmployeeExperienceDtlId(employeePoid, dto.getDetRowId());
                experienceRepository.deleteById(id);
            }
        }
    }

    private void applyDocumentDetails(Long employeePoid, List<EmployeeDocumentDtlRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeDocumentDtl> existing = documentRepository.findByEmployeePoid(employeePoid);
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeDocumentDtl::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeDocumentDtlRequestDto dto : dtos) {

            if (dto.getEmployeePoid() != null) {
                if (!masterRepository.existsByEmployeePoid(dto.getEmployeePoid())) {
                    throw new ResourceNotFoundException("Employee", "Employee Poid", dto.getEmployeePoid());
                }
            }

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
                documentRepository.save(entity);
            } else if (action == ActionType.isUpdated) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for document isUpdated action");
                }

                HrEmployeeDocumentDtlId id = new HrEmployeeDocumentDtlId(employeePoid, dto.getDetRowId());
                HrEmployeeDocumentDtl entity = documentRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Document", DET_ROW_ID, dto.getDetRowId()));

                entity.setDocName(dto.getDocName());
                entity.setExpiryDate(dto.getExpiryDate());
                entity.setRemarks(dto.getRemarks());
                documentRepository.save(entity);
            } else if (action == ActionType.isDeleted) {
                if (dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("detRowId is required for document isDeleted action");
                }
                HrEmployeeDocumentDtlId id = new HrEmployeeDocumentDtlId(employeePoid, dto.getDetRowId());
                documentRepository.deleteById(id);
            }
        }
    }

    private void applyLeaveHistoryDetails(Long employeePoid, List<EmployeeLeaveHistoryRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<HrEmployeeLeaveHistory> existing = leaveHistoryRepository.findByEmployeePoid(employeePoid);

        long nextLeaveHistPoid = existing.stream().mapToLong(HrEmployeeLeaveHistory::getLeaveHistPoid).max().orElse(0L) + 1;
        long nextDetRowId = existing.stream().mapToLong(HrEmployeeLeaveHistory::getDetRowId).max().orElse(0L) + 1;

        for (EmployeeLeaveHistoryRequestDto dto : dtos) {

            if (dto.getEmployeePoid() != null) {
                if (!masterRepository.existsByEmployeePoid(dto.getEmployeePoid())) {
                    throw new ResourceNotFoundException("Employee", "Employee Poid", dto.getEmployeePoid());
                }
            }

            if (dto == null || dto.getActionType() == null || dto.getActionType() == ActionType.noChange) continue;

            ActionType action = dto.getActionType();
            if (action == ActionType.isCreated) {
                long leaveHistPoid = dto.getLeaveHistPoid() != null ? dto.getLeaveHistPoid() : nextLeaveHistPoid++;
                long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : nextDetRowId++;

                nextLeaveHistPoid = Math.max(nextLeaveHistPoid, leaveHistPoid + 1);
                nextDetRowId = Math.max(nextDetRowId, detRowId + 1);

                HrEmployeeLeaveHistory entity = new HrEmployeeLeaveHistory();
                entity.setLeaveHistPoid(leaveHistPoid);
                entity.setDetRowId(detRowId);
                entity.setEmployeePoid(employeePoid);

                entity.setCompanyPoid(dto.getCompanyPoid() != null ? dto.getCompanyPoid() : UserContext.getCompanyPoid());
                entity.setGroupPoid(dto.getGroupPoid() != null ? dto.getGroupPoid() : UserContext.getGroupPoid());
                entity.setDeptPoid(dto.getDeptPoid());
                entity.setEmployeeName(dto.getEmployeeName());

                entity.setLeaveStartDate(dto.getLeaveStartDate());
                entity.setRejoinDate(dto.getRejoinDate());
                entity.setReffNo(dto.getReffNo());
                entity.setRemarks(dto.getRemarks());
                entity.setDeleted(dto.getDeleted());
                entity.setLeaveType(dto.getLeaveType());
                entity.setLeaveDays(dto.getLeaveDays());
                entity.setAnnualLeaveType(dto.getAnnualLeaveType());
                entity.setSourceDocId(dto.getSourceDocId());
                entity.setSourceDocPoid(dto.getSourceDocPoid());
                entity.setEmergencyLeaveType(dto.getEmergencyLeaveType());
                entity.setExpRejoinDate(dto.getExpRejoinDate());
                entity.setSplLeaveTypes(dto.getSplLeaveTypes());
                entity.setEligibleLeaveDays(dto.getEligibleLeaveDays());
                entity.setTicketIssuedCount(dto.getTicketIssuedCount());
                entity.setTicketTillDate(dto.getTicketTillDate());
                entity.setTicketIssueType(dto.getTicketIssueType());

                leaveHistoryRepository.save(entity);
            } else if (action == ActionType.isUpdated) {
                if (dto.getLeaveHistPoid() == null || dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("leaveHistPoid and detRowId are required for leaveHistory isUpdated action");
                }

                HrEmployeeLeaveHistoryId id = new HrEmployeeLeaveHistoryId(dto.getLeaveHistPoid(), dto.getDetRowId());
                HrEmployeeLeaveHistory entity = leaveHistoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Leave History", "leaveHistPoid/detRowId", dto.getLeaveHistPoid()));

                entity.setEmployeePoid(employeePoid);
                entity.setCompanyPoid(dto.getCompanyPoid() != null ? dto.getCompanyPoid() : entity.getCompanyPoid());
                entity.setGroupPoid(dto.getGroupPoid() != null ? dto.getGroupPoid() : entity.getGroupPoid());
                entity.setDeptPoid(dto.getDeptPoid());
                entity.setEmployeeName(dto.getEmployeeName());
                entity.setLeaveStartDate(dto.getLeaveStartDate());
                entity.setRejoinDate(dto.getRejoinDate());
                entity.setReffNo(dto.getReffNo());
                entity.setRemarks(dto.getRemarks());
                entity.setDeleted(dto.getDeleted());
                entity.setLeaveType(dto.getLeaveType());
                entity.setLeaveDays(dto.getLeaveDays());
                entity.setAnnualLeaveType(dto.getAnnualLeaveType());
                entity.setSourceDocId(dto.getSourceDocId());
                entity.setSourceDocPoid(dto.getSourceDocPoid());
                entity.setEmergencyLeaveType(dto.getEmergencyLeaveType());
                entity.setExpRejoinDate(dto.getExpRejoinDate());
                entity.setSplLeaveTypes(dto.getSplLeaveTypes());
                entity.setEligibleLeaveDays(dto.getEligibleLeaveDays());
                entity.setTicketIssuedCount(dto.getTicketIssuedCount());
                entity.setTicketTillDate(dto.getTicketTillDate());
                entity.setTicketIssueType(dto.getTicketIssueType());

                leaveHistoryRepository.save(entity);
            } else if (action == ActionType.isDeleted) {
                if (dto.getLeaveHistPoid() == null || dto.getDetRowId() == null) {
                    throw new IllegalArgumentException("leaveHistPoid and detRowId are required for leaveHistory isDeleted action");
                }
                HrEmployeeLeaveHistoryId id = new HrEmployeeLeaveHistoryId(dto.getLeaveHistPoid(), dto.getDetRowId());
                leaveHistoryRepository.deleteById(id);
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
        sortableFieldMap.put(HR_EMPLOYEE_MASTER_POID_FIELD, "employeePoid");
        sortableFieldMap.put("EMPLOYEE_NAME", "employeeName");
        sortableFieldMap.put("EMPLOYEE_NAME2", "employeeName2");
        sortableFieldMap.put("DESIGNATION_POID", "designationPoid");
        sortableFieldMap.put("LOCATION_POID", "locationPoid");
        sortableFieldMap.put("DEPARTMENT_POID", "departmentPoid");
        sortableFieldMap.put("JOIN_DATE", "joinDate");
        sortableFieldMap.put("MOBILE", "mobile");
        sortableFieldMap.put("ACTIVE", "active");

        List<org.springframework.data.domain.Sort.Order> safeOrders = pageable.getSort().stream()
                .map(order -> {
                    String property = order.getProperty();
                    String mapped = sortableFieldMap.get(property.toUpperCase());
                    if (mapped == null && !property.isEmpty() && Character.isLowerCase(property.charAt(0))) {
                        mapped = property;
                    }
                    if (mapped == null) {
                        mapped = "employeePoid";
                    }
                    return new org.springframework.data.domain.Sort.Order(order.getDirection(), mapped);
                })
                .collect(Collectors.toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(safeOrders));
    }

    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        JasperReport mainReport = printService.load("EmployeeDetailsReportWithSalary.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
}

