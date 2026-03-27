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
import com.asg.hr.employeemaster.enums.ActionType;
import com.asg.hr.employeemaster.repository.*;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.locationmaster.repository.GlobalLocationMasterRepository;
import com.asg.hr.nationality.repository.HrNationalityRepository;
import com.asg.hr.religion.repository.ReligionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeMasterServiceImpl implements EmployeeMasterService {

    private static final String HR_EMPLOYEE_MASTER_TABLE = "HR_EMPLOYEE_MASTER";
    private static final String HR_EMPLOYEE_MASTER_POID_FIELD = "EMPLOYEE_POID";
    private static final String DET_ROW_ID = "DET_ROW_ID";
    private static final String EMPLOYEE = "Employee";

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

    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

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

        return toResponseDto(entity);
    }

    @Override
    public EmployeeMasterResponseDto createEmployee(EmployeeMasterRequestDto requestDto) {

        if (requestDto.getAirSectorPoid() != null) {
            if (!airsectorRepository.existsByAirsecPoid(requestDto.getAirSectorPoid())) {
                throw new ResourceNotFoundException("Air Sector", "Air Sector Poid", requestDto.getAirSectorPoid());
            }
        }
        if (requestDto.getDepartmentPoid() != null) {
            if (!hrDepartmentMasterRepository.existsByDeptPoid(requestDto.getDepartmentPoid())) {
                throw new ResourceNotFoundException("Department", "Department Poid", requestDto.getDepartmentPoid());
            }
        }
        if (requestDto.getNationalityPoid() != null) {
            if (Boolean.FALSE.equals(hrNationalityRepository.existsByNationPoid(requestDto.getNationalityPoid()))) {
                throw new ResourceNotFoundException("Nationality", "Nationality Poid", requestDto.getNationalityPoid());
            }
        }
        if (requestDto.getReligionPoid() != null) {
            if (!religionRepository.existsByReligionPoid(requestDto.getReligionPoid())) {
                throw new ResourceNotFoundException("Religion", "Religion Poid", requestDto.getReligionPoid());
            }
        }
        if (requestDto.getDesignationPoid() != null) {
            if (!designationRepository.existsByDesigPoid(requestDto.getDesignationPoid())) {
                throw new ResourceNotFoundException("Designation", "Designation Poid", requestDto.getDesignationPoid());
            }
        }
        if (requestDto.getShiftPoid() != null) {
            if (!globalShiftMasterRepository.existsByShiftPoid(requestDto.getShiftPoid())) {
                throw new ResourceNotFoundException("Shift", "Shift Poid", requestDto.getShiftPoid());
            }
        }
        if (requestDto.getDiscontinued() != null) {
            if (!globalFixedVariablesRepository.existsByVariableName(requestDto.getDiscontinued())) {
                throw new ResourceNotFoundException("Fixed Variable", "View Using", requestDto.getDiscontinued());
            }
        }
        if (requestDto.getLocationPoid() != null) {
            if (!locationMasterRepository.existsByLocationPoid(requestDto.getLocationPoid())) {
                throw new ResourceNotFoundException("Location", "Location Poid", requestDto.getLocationPoid());
            }
        }
        if (requestDto.getCrPoid() != null) {
            if (!crMasterRepository.existsByCrPoid(requestDto.getCrPoid())) {
                throw new ResourceNotFoundException("Admin Cr Master", "Cr Poid", requestDto.getCrPoid());
            }
        }
        if (requestDto.getMobile() != null) {
            if (masterRepository.existsByMobile(requestDto.getMobile())) {
                throw new ResourceAlreadyExistsException("Mobile", requestDto.getMobile());
            }
        }
        if (requestDto.getFirstName() != null) {
            if (masterRepository.existsByEmployeeName(requestDto.getFirstName())) {
                throw new ResourceAlreadyExistsException("Name", requestDto.getFirstName());
            }
        }
        if (requestDto.getEmpGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getEmpGlPoid());
        }
        if (requestDto.getPettyCashGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getPettyCashGlPoid());
        }

        HrEmployeeMaster entity = new HrEmployeeMaster();
        applyHeaderFields(entity, requestDto);
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
        return toResponseDto(saved);
    }

    @Override
    public EmployeeMasterResponseDto updateEmployee(Long employeePoid, EmployeeMasterRequestDto requestDto) {

        HrEmployeeMaster existing = masterRepository.findByEmployeePoid(employeePoid)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE, HR_EMPLOYEE_MASTER_POID_FIELD, employeePoid));

        if (requestDto.getHod() != null && employeePoid.equals(requestDto.getHod())) {
            throw new ValidationException("Should not select current employee as direct supervisor");
        }
        if (requestDto.getAirSectorPoid() != null) {
            if (!airsectorRepository.existsByAirsecPoid(requestDto.getAirSectorPoid())) {
                throw new ResourceNotFoundException("Air Sector", "Air Sector Poid", requestDto.getAirSectorPoid());
            }
        }
        if (requestDto.getDepartmentPoid() != null) {
            if (!hrDepartmentMasterRepository.existsByDeptPoid(requestDto.getDepartmentPoid())) {
                throw new ResourceNotFoundException("Department", "Department Poid", requestDto.getDepartmentPoid());
            }
        }
        if (requestDto.getNationalityPoid() != null) {
            if (Boolean.FALSE.equals(hrNationalityRepository.existsByNationPoid(requestDto.getNationalityPoid()))) {
                throw new ResourceNotFoundException("Nationality", "Nationality Poid", requestDto.getNationalityPoid());
            }
        }
        if (requestDto.getReligionPoid() != null) {
            if (!religionRepository.existsByReligionPoid(requestDto.getReligionPoid())) {
                throw new ResourceNotFoundException("Religion", "Religion Poid", requestDto.getReligionPoid());
            }
        }
        if (requestDto.getDesignationPoid() != null) {
            if (!designationRepository.existsByDesigPoid(requestDto.getDesignationPoid())) {
                throw new ResourceNotFoundException("Designation", "Designation Poid", requestDto.getDesignationPoid());
            }
        }
        if (requestDto.getShiftPoid() != null) {
            if (!globalShiftMasterRepository.existsByShiftPoid(requestDto.getShiftPoid())) {
                throw new ResourceNotFoundException("Shift", "Shift Poid", requestDto.getShiftPoid());
            }
        }
        if (requestDto.getDiscontinued() != null) {
            if (!globalFixedVariablesRepository.existsByVariableName(requestDto.getDiscontinued())) {
                throw new ResourceNotFoundException("Fixed Variable", "View Using", requestDto.getDiscontinued());
            }
        }
        if (requestDto.getLocationPoid() != null) {
            if (!locationMasterRepository.existsByLocationPoid(requestDto.getLocationPoid())) {
                throw new ResourceNotFoundException("Location", "Location Poid", requestDto.getLocationPoid());
            }
        }
        if (requestDto.getCrPoid() != null) {
            if (!crMasterRepository.existsByCrPoid(requestDto.getCrPoid())) {
                throw new ResourceNotFoundException("Admin Cr Master", "Cr Poid", requestDto.getCrPoid());
            }
        }
        if (requestDto.getMobile() != null) {
            if (masterRepository.existsByMobileAndEmployeePoidNot(requestDto.getMobile(), employeePoid)) {
                throw new ResourceAlreadyExistsException("Mobile", requestDto.getMobile());
            }
        }
        if (requestDto.getFirstName() != null) {
            if (masterRepository.existsByEmployeeNameAndEmployeePoidNot(requestDto.getFirstName(), employeePoid)) {
                throw new ResourceAlreadyExistsException("Name", requestDto.getFirstName());
            }
        }
        if (requestDto.getEmpGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getEmpGlPoid());
        }
        if (requestDto.getPettyCashGlPoid() != null) {
            glMasterServiceClient.findById(requestDto.getPettyCashGlPoid());
        }

        HrEmployeeMaster oldEntity = new HrEmployeeMaster();
        // Copy full entity snapshot for accurate audit logging (no manual audit field assignment).
        BeanUtils.copyProperties(existing, oldEntity);

        // Apply header changes (photo is updated via separate endpoint).
        applyHeaderFields(existing, requestDto);

        HrEmployeeMaster saved = masterRepository.save(existing);

        // Child tables: apply actionType updates.
        applyDependents(saved.getEmployeePoid(), requestDto.getDependentsDetails());
        applyLmraDetails(saved.getEmployeePoid(), requestDto.getLmraDetails());
        applyExperienceDetails(saved.getEmployeePoid(), requestDto.getExperienceDetails());
        applyDocumentDetails(saved.getEmployeePoid(), requestDto.getDocumentDetails());

        loggingService.logChanges(oldEntity, saved, HrEmployeeMaster.class, UserContext.getDocumentId(), employeePoid.toString(), LogDetailsEnum.MODIFIED, HR_EMPLOYEE_MASTER_POID_FIELD);
        return toResponseDto(saved);
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

    private EmployeeMasterResponseDto toResponseDto(HrEmployeeMaster entity) {
        Long employeePoid = entity.getEmployeePoid();

        List<HrEmployeeDependentsDtl> dependents = dependentRepository.findByEmployeePoid(employeePoid);
        List<HrEmpDepndtsLmraDtls> lmraDetails = lmraRepository.findByEmployeePoid(employeePoid);
        List<HrEmployeeExperienceDtl> experience = experienceRepository.findByEmployeePoid(employeePoid);
        List<HrEmployeeDocumentDtl> documents = documentRepository.findByEmployeePoid(employeePoid);
        List<HrEmployeeLeaveHistory> leaveHistory = leaveHistoryRepository.findByEmployeePoid(employeePoid);

        return EmployeeMasterResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .groupPoid(entity.getGroupPoid())

                .employeeCode(entity.getEmployeeCode())
                .employeeName(entity.getEmployeeName())
                .employeeName2(entity.getEmployeeName2())
                .baseGroup(entity.getBaseGroup())

                .companyPoid(entity.getCompanyPoid())
                .locationPoid(entity.getLocationPoid())
                .departmentPoid(entity.getDepartmentPoid())
                .designationPoid(entity.getDesignationPoid())

                .photo(entity.getPhoto())
                .joinDate(entity.getJoinDate())
                .nationalityPoid(entity.getNationalityPoid())
                .gender(entity.getGender())
                .maritalStatus(entity.getMaritalStatus())
                .religionPoid(entity.getReligionPoid())
                .dateOfBirth(entity.getDateOfBirth())
                .presentAddress(entity.getPresentAddress())
                .permanentAddress(entity.getPermanentAddress())
                .postalAddress(entity.getPostalAddress())
                .homeCountryPhone(entity.getHomeCountryPhone())

                .mobile(entity.getMobile())
                .personalEmail(entity.getPersonalEmail())
                .businessEmail(entity.getBusinessEmail())
                .bloodGroup(entity.getBloodGroup())
                .emergencyContactPerson(entity.getEmergencyContactPerson())
                .emergencyContactNo(entity.getEmergencyContactNo())

                .serviceStartDate(entity.getServiceStartDate())
                .serviceType(entity.getServiceType())
                .contractStart(entity.getContractStart())
                .contractEnd(entity.getContractEnd())
                .probation(entity.getProbation())
                .noticePeriod(entity.getNoticePeriod())
                .directSupervisorPoid(entity.getDirectSupervisorPoid())
                .loginUserPoid(entity.getLoginUserPoid())
                .jobDescription(entity.getJobDescription())
                .airSectorPoid(entity.getAirSectorPoid())
                .ticketPeriod(entity.getTicketPeriod())
                .noOfTickets(entity.getNoOfTickets())
                .accessCardIssued(entity.getAccessCardIssued())
                .discontinued(entity.getDiscontinued())
                .discontinuedDate(entity.getDiscontinuedDate())
                .reason(entity.getReason())
                .otherReasons(entity.getOtherReasons())

                .basicSalary(entity.getBasicSalary())
                .netSalary(entity.getNetSalary())
                .currencyPoid(entity.getCurrencyPoid())
                .paymentMethod(entity.getPaymentMethod())

                .bankPoid(entity.getBankPoid())
                .accountNo(entity.getAccountNo())
                .iban(entity.getIban())
                .registeredSalary(entity.getRegisteredSalary())
                .lastIncrementDate(entity.getLastIncrementDate())
                .nextIncrementDate(entity.getNextIncrementDate())
                .holdSalary(entity.getHoldSalary())
                .holdReason(entity.getHoldReason())
                .crPoid(entity.getCrPoid())
                .passportNo(entity.getPassportNo())
                .issuedDate(entity.getIssuedDate())
                .expiryDate(entity.getExpiryDate())
                .placeOfIssue(entity.getPlaceOfIssue())
                .passportPossessedBy(entity.getPassportPossessedBy())
                .gosiNo(entity.getGosiNo())
                .gosiReturnAmount(entity.getGosiReturnAmount())
                .cprNo(entity.getCprNo())
                .cprExpiryDate(entity.getCprExpiryDate())
                .cprOccupation(entity.getCprOccupation())
                .medicalInsurance(entity.getMedicalInsurance())
                .medicalInsuranceExpiryDate(entity.getMedicalInsuranceExpiryDate())
                .rpNo(entity.getRpNo())
                .rpStartDate(entity.getRpStartDate())
                .rpExpiryDate(entity.getRpExpiryDate())

                .active(entity.getActive())
                .seqNo(entity.getSeqNo())
                .employeeBiomatrixId(entity.getEmployeeBiomatrixId())
                .otApplicable(entity.getOtApplicable())
                .insuranceNominee(entity.getInsuranceNominee())
                .insuranceNomineeRelation(entity.getInsuranceNomineeRelation())
                .shiftPoid(entity.getShiftPoid())
                .currencyCode(entity.getCurrencyCode())
                .deleted(entity.getDeleted())
                .drivingLicExp(entity.getDrivingLicExp())
                .nomineeContactDtl(entity.getNomineeContactDtl())
                .displayName(entity.getDisplayName())
                .empGlPoid(entity.getEmpGlPoid())
                .insuNominee2(entity.getInsuNominee2())
                .bankRegisterEmployeeNo(entity.getBankRegisterEmployeeNo())
                .probationBenefits(entity.getProbationBenefits())
                .probationCompletedOn(entity.getProbationCompletedOn())
                .lifeInsurance(entity.getLifeInsurance())
                .attendanceFromLog(entity.getAttendanceFromLog())
                .managementStaff(entity.getManagementStaff())
                .extNo(entity.getExtNo())
                .attendanceLateCheck(entity.getAttendanceLateCheck())
                .attendanceLunchCheck(entity.getAttendanceLunchCheck())
                .passportRemarks(entity.getPassportRemarks())
                .placeOfHire(entity.getPlaceOfHire())
                .pettyCashGlPoid(entity.getPettyCashGlPoid())
                .attendanceCheck(entity.getAttendanceCheck())
                .recruitmentSource(entity.getRecruitmentSource())
                .recruitmentDetails(entity.getRecruitmentDetails())
                .recruitmentBudgeted(entity.getRecruitmentBudgeted())
                .recruitmentReason(entity.getRecruitmentReason())
                .ot1Limit(entity.getOt1Limit())
                .ot2Limit(entity.getOt2Limit())
                .shortHours(entity.getShortHours())
                .codeOfConductSigned(entity.getCodeOfConductSigned())
                .orientation(entity.getOrientation())
                .orientationRemarks(entity.getOrientationRemarks())
                .freeLunch(entity.getFreeLunch())
                .staffAccommodation(entity.getStaffAccommodation())
                .actualDob(entity.getActualDob())

                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())

                .dependentsDetails(dependents.stream().map(this::toDependentsResponseDto).collect(Collectors.toList()))
                .lmraDetails(lmraDetails.stream().map(this::toLmraResponseDto).collect(Collectors.toList()))
                .experienceDetails(experience.stream().map(this::toExperienceResponseDto).collect(Collectors.toList()))
                .documentDetails(documents.stream().map(this::toDocumentResponseDto).collect(Collectors.toList()))
                .leaveHistoryDetails(leaveHistory.stream().map(this::toLeaveHistoryResponseDto).collect(Collectors.toList()))
                .build();
    }

    private void applyHeaderFields(HrEmployeeMaster entity, EmployeeMasterRequestDto requestDto) {

        entity.setLocationPoid(requestDto.getLocationPoid());
        entity.setDepartmentPoid(requestDto.getDepartmentPoid());
        entity.setDesignationPoid(requestDto.getDesignationPoid());

        entity.setEmployeeName(requestDto.getFirstName());
        entity.setEmployeeName2(requestDto.getLastName());
        entity.setBaseGroup(requestDto.getBaseGroup());

        entity.setJoinDate(requestDto.getJoinDate());
        entity.setNationalityPoid(requestDto.getNationalityPoid());
        entity.setReligionPoid(requestDto.getReligionPoid());
        entity.setGender(requestDto.getGender());
        entity.setMaritalStatus(requestDto.getMaritalStatus());
        entity.setDateOfBirth(requestDto.getDateOfBirth());
        entity.setPresentAddress(requestDto.getPresentAddress());
        entity.setPermanentAddress(requestDto.getPermanentAddress());
        entity.setPostalAddress(requestDto.getPostalAddress());
        entity.setHomeCountryPhone(requestDto.getHomeCountryPhone());

        entity.setMobile(requestDto.getMobile());
        entity.setPersonalEmail(requestDto.getPersonalEmail());
        entity.setBusinessEmail(requestDto.getBusinessEmail());
        entity.setBloodGroup(requestDto.getBloodGroup());
        entity.setEmergencyContactPerson(requestDto.getEmergencyContactPerson());
        entity.setEmergencyContactNo(requestDto.getEmergencyContactNo());

        entity.setServiceStartDate(requestDto.getServiceStartDate());
        entity.setServiceType(requestDto.getServiceType());
        entity.setContractStart(requestDto.getContractStart());
        entity.setContractEnd(requestDto.getContractEnd());
        entity.setProbation(requestDto.getProbation());
        entity.setNoticePeriod(requestDto.getNoticePeriod());
        entity.setDirectSupervisorPoid(requestDto.getHod());
        entity.setLoginUserPoid(UserContext.getUserPoid());
        entity.setJobDescription(requestDto.getJobDescription());
        entity.setAirSectorPoid(requestDto.getAirSectorPoid());
        entity.setTicketPeriod(requestDto.getTicketPeriod());
        entity.setNoOfTickets(requestDto.getNoOfTickets());
        entity.setAccessCardIssued(requestDto.getAccessCardIssued());
        entity.setDiscontinued(requestDto.getDiscontinued());
        entity.setDiscontinuedDate(requestDto.getDiscontinuedDate());
        entity.setReason(requestDto.getReason());
        entity.setOtherReasons(requestDto.getOtherReasons());

        entity.setBasicSalary(requestDto.getBasicSalary());
        entity.setNetSalary(requestDto.getNetSalary());
        entity.setCurrencyPoid(requestDto.getCurrencyPoid());
        entity.setPaymentMethod(requestDto.getPaymentMethod());
        entity.setBankPoid(requestDto.getBankPoid());
        entity.setAccountNo(requestDto.getAccountNo());
        entity.setIban(requestDto.getIban());
        entity.setRegisteredSalary(requestDto.getRegisteredSalary());
        entity.setLastIncrementDate(requestDto.getLastIncrementDate());
        entity.setNextIncrementDate(requestDto.getNextIncrementDate());
        entity.setHoldSalary(requestDto.getHoldSalary());
        entity.setHoldReason(requestDto.getHoldReason());

        entity.setCrPoid(requestDto.getCrPoid());
        entity.setPassportNo(requestDto.getPassportNo());
        entity.setIssuedDate(requestDto.getIssuedDate());
        entity.setExpiryDate(requestDto.getExpiryDate());
        entity.setPlaceOfIssue(requestDto.getPlaceOfIssue());
        entity.setPassportPossessedBy(requestDto.getPassportPossessedBy());
        entity.setGosiNo(requestDto.getGosiNo());
        entity.setGosiReturnAmount(requestDto.getGosiReturnAmount());
        entity.setCprNo(requestDto.getCprNo());
        entity.setCprExpiryDate(requestDto.getCprExpiryDate());
        entity.setCprOccupation(requestDto.getCprOccupation());
        entity.setMedicalInsurance(requestDto.getMedicalInsurance());
        entity.setMedicalInsuranceExpiryDate(requestDto.getMedicalInsuranceExpiryDate());
        entity.setRpNo(requestDto.getRpNo());
        entity.setRpStartDate(requestDto.getRpStartDate());
        entity.setRpExpiryDate(requestDto.getRpExpiryDate());

        entity.setActive(requestDto.getActive());
        entity.setSeqNo(requestDto.getSeqNo());
        entity.setEmployeeBiomatrixId(requestDto.getEmployeeBiomatrixId());
        entity.setOtApplicable(requestDto.getOtApplicable());
        entity.setInsuranceNominee(requestDto.getInsuranceNominee());
        entity.setInsuranceNomineeRelation(requestDto.getInsuranceNomineeRelation());
        entity.setShiftPoid(requestDto.getShiftPoid());
        entity.setCurrencyCode(requestDto.getCurrencyCode());
        entity.setDeleted(requestDto.getDeleted() != null ? requestDto.getDeleted() : "N");
        entity.setDrivingLicExp(requestDto.getDrivingLicExp());
        entity.setNomineeContactDtl(requestDto.getNomineeContactDtl());
        entity.setDisplayName(requestDto.getDisplayName());
        entity.setEmpGlPoid(requestDto.getEmpGlPoid());
        entity.setInsuNominee2(requestDto.getInsuNominee2());
        entity.setBankRegisterEmployeeNo(requestDto.getBankRegisterEmployeeNo());
        entity.setProbationBenefits(requestDto.getProbationBenefits());
        entity.setProbationCompletedOn(requestDto.getProbationCompletedOn());
        entity.setLifeInsurance(requestDto.getLifeInsurance());
        entity.setAttendanceFromLog(requestDto.getAttendanceFromLog());
        entity.setManagementStaff(requestDto.getManagementStaff());
        entity.setExtNo(requestDto.getExtNo());
        entity.setAttendanceLateCheck(requestDto.getAttendanceLateCheck());
        entity.setAttendanceLunchCheck(requestDto.getAttendanceLunchCheck());
        entity.setPassportRemarks(requestDto.getPassportRemarks());
        entity.setPlaceOfHire(requestDto.getPlaceOfHire());
        entity.setPettyCashGlPoid(requestDto.getPettyCashGlPoid());
        entity.setAttendanceCheck(requestDto.getAttendanceCheck());
        entity.setRecruitmentSource(requestDto.getRecruitmentSource());
        entity.setRecruitmentDetails(requestDto.getRecruitmentDetails());
        entity.setRecruitmentBudgeted(requestDto.getRecruitmentBudgeted());
        entity.setRecruitmentReason(requestDto.getRecruitmentReason());
        entity.setOt1Limit(requestDto.getOt1Limit());
        entity.setOt2Limit(requestDto.getOt2Limit());
        entity.setShortHours(requestDto.getShortHours());
        entity.setCodeOfConductSigned(requestDto.getCodeOfConductSigned());
        entity.setOrientation(requestDto.getOrientation());
        entity.setOrientationRemarks(requestDto.getOrientationRemarks());
        entity.setFreeLunch(requestDto.getFreeLunch());
        entity.setStaffAccommodation(requestDto.getStaffAccommodation());
        entity.setActualDob(requestDto.getActualDob());
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

    private EmployeeDependentsDtlResponseDto toDependentsResponseDto(HrEmployeeDependentsDtl entity) {
        return EmployeeDependentsDtlResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .name(entity.getName())
                .dateOfBirth(entity.getDateOfBirth())
                .relation(entity.getRelation())
                .gender(entity.getGender())
                .nationality(entity.getNationality())
                .passportNo(entity.getPassportNo())
                .ppExpiryDate(entity.getPpExpiryDate())
                .cprNo(entity.getCprNo())
                .cprExpiry(entity.getCprExpiry())
                .insuDetails(entity.getInsuDetails())
                .insuStartDt(entity.getInsuStartDt())
                .sponsor(entity.getSponsor())
                .rpExpiry(entity.getRpExpiry())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeDepndtsLmraDtlsResponseDto toLmraResponseDto(HrEmpDepndtsLmraDtls entity) {
        return EmployeeDepndtsLmraDtlsResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .expatCpr(entity.getExpatCpr())
                .expatPp(entity.getExpatPp())
                .nationality(entity.getNationality())
                .primaryCpr(entity.getPrimaryCpr())
                .wpType(entity.getWpType())
                .permitMonths(entity.getPermitMonths())
                .expatName(entity.getExpatName())
                .expatGender(entity.getExpatGender())
                .wpExpiryDate(entity.getWpExpiryDate())
                .ppExpiryDate(entity.getPpExpiryDate())
                .expatCurrentStatus(entity.getExpatCurrentStatus())
                .wpStatus(entity.getWpStatus())
                .inOutStatus(entity.getInOutStatus())
                .offenseClassification(entity.getOffenseClassification())
                .offenceCode(entity.getOffenceCode())
                .offenceDescription(entity.getOffenceDescription())
                .intention(entity.getIntention())
                .allowMobility(entity.getAllowMobility())
                .mobilityInProgress(entity.getMobilityInProgress())
                .rpCancelled(entity.getRpCancelled())
                .rpCancellationReason(entity.getRpCancellationReason())
                .photo(entity.getPhoto())
                .signature(entity.getSignature())
                .fingerPrint(entity.getFingerPrint())
                .healthCheckResult(entity.getHealthCheckResult())
                .additionalBhPermit(entity.getAdditionalBhPermit())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeExperienceDtlResponseDto toExperienceResponseDto(HrEmployeeExperienceDtl entity) {
        return EmployeeExperienceDtlResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .employer(entity.getEmployer())
                .countryLocation(entity.getCountryLocation())
                .fromDate(entity.getFromDate())
                .toDate(entity.getToDate())
                .months(entity.getMonths())
                .designation(entity.getDesignation())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeDocumentDtlResponseDto toDocumentResponseDto(HrEmployeeDocumentDtl entity) {
        return EmployeeDocumentDtlResponseDto.builder()
                .employeePoid(entity.getEmployeePoid())
                .detRowId(entity.getDetRowId())
                .docName(entity.getDocName())
                .expiryDate(entity.getExpiryDate())
                .remarks(entity.getRemarks())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private EmployeeLeaveHistoryResponseDto toLeaveHistoryResponseDto(HrEmployeeLeaveHistory entity) {
        return EmployeeLeaveHistoryResponseDto.builder()
                .leaveHistPoid(entity.getLeaveHistPoid())
                .detRowId(entity.getDetRowId())
                .companyPoid(entity.getCompanyPoid())
                .groupPoid(entity.getGroupPoid())
                .deptPoid(entity.getDeptPoid())
                .employeePoid(entity.getEmployeePoid())
                .employeeName(entity.getEmployeeName())
                .leaveStartDate(entity.getLeaveStartDate())
                .rejoinDate(entity.getRejoinDate())
                .reffNo(entity.getReffNo())
                .remarks(entity.getRemarks())
                .deleted(entity.getDeleted())
                .leaveType(entity.getLeaveType())
                .leaveDays(entity.getLeaveDays())
                .annualLeaveType(entity.getAnnualLeaveType())
                .sourceDocId(entity.getSourceDocId())
                .sourceDocPoid(entity.getSourceDocPoid())
                .emergencyLeaveType(entity.getEmergencyLeaveType())
                .expRejoinDate(entity.getExpRejoinDate())
                .splLeaveTypes(entity.getSplLeaveTypes())
                .eligibleLeaveDays(entity.getEligibleLeaveDays())
                .ticketIssuedCount(entity.getTicketIssuedCount())
                .ticketTillDate(entity.getTicketTillDate())
                .ticketIssueType(entity.getTicketIssueType())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}

