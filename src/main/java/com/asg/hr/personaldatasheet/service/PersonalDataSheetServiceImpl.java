package com.asg.hr.personaldatasheet.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetRequestDto;
import com.asg.hr.personaldatasheet.dto.PersonalDataSheetResponseDto;
import com.asg.hr.personaldatasheet.entity.*;
import com.asg.hr.personaldatasheet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalDataSheetServiceImpl implements PersonalDataSheetService {

    // Constants for duplicated string literals
    private static final String ACTION_CREATED = "ISCREATED";
    private static final String ACTION_UPDATED = "ISUPDATED";
    private static final String ACTION_DELETED = "ISDELETED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String DOC_ID_DEFAULT = "800-112";
    private static final String DET_ROW_ID_FIELD = "detRowId";
    private static final String TRANSACTION_POID_FIELD = "TRANSACTION_POID";
    private static final String KEY_ID_FORMAT = "KeyId = TRANSACTION_POID:%s DET_ROW_ID:%s";
    private static final String LOG_DETAIL_DEPENDENT = "Row Created on Personal Data Dependent with detRowId: %s";
    private static final String LOG_DETAIL_EMERGENCY = "Row Created on Personal Data Emergency Contact with detRowId: %s";
    private static final String LOG_DETAIL_NOMINEE = "Row Created on Personal Data Nominee with detRowId: %s";
    private static final String LOG_DETAIL_POLICY = "Row Created on Personal Data Policy with detRowId: %s";
    private static final String ERROR_NOT_FOUND = "Personal data sheet not found with ID: ";
    private static final String TABLE_NAME = "HR_PERSONAL_DATA_HDR";
    private static final String EMPLOYEE_NAME_PASSPORT = "EMPLOYEE_NAME_PASSPORT";
    private static final String DEPENDENT_DETAIL = "Dependent Detail";
    private static final String EMERGENCY_CONTACT_DETAIL = "Emergency Contact Detail";
    private static final String NOMINEE_DETAIL = "Nominee Detail";
    private static final String POLICY_DETAIL = "Policy Detail";
    private static final String UNKNOWN_ACTION_LOG = "Unknown action type: {} for {} with detRowId: {}";
    private static final String LOG_CREATE_MSG = "Creating personal data sheet for employee: {}";
    private static final String LOG_FETCH_MSG = "Fetching personal data sheet by ID: {}";
    private static final String LOG_UPDATE_MSG = "Updating personal data sheet: {}";
    private static final String LOG_DELETE_MSG = "Deleting personal data sheet: {}";
    private static final String LOG_LIST_MSG = "Listing personal data sheets with filters";
    private static final String LOG_USER_EMPLOYEE_MSG = "Getting login user employee ID for user: {}";
    private static final String LOG_USER_POLICIES_MSG = "Loading user policies for employee: {}";

    private final HrPersonalDataHdrRepository repository;
    private final HrPersonalDataDependentRepository dependentRepository;
    private final HrPersonalDataEmergencyRepository emergencyRepository;
    private final HrPersonalDataNomineeRepository nomineeRepository;
    private final HrPersonalDataPoliciesRepository policiesRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final PersonalDataSheetValidator validator;
    private final PersonalDataSheetProcedureRepository procedureRepository;

    @Override
    @Transactional
    public PersonalDataSheetResponseDto create(PersonalDataSheetRequestDto request) {
        log.info(LOG_CREATE_MSG, request.getEmployeePoid());

        validator.validateRequest(request);

        HrPersonalDataHdr entity = mapToEntity(request);
        entity.setGroupPoid(UserContext.getGroupPoid());
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setTransactionDate(LocalDate.now());
        entity.setStatus(STATUS_DRAFT);
        entity.setCreatedBy(UserContext.getUserId());

        // Save header first without child entities
        entity = repository.save(entity);

        // Now save child entities using individual repositories (asg-finance pattern)
        String docId = UserContext.getDocumentId();
        if (request.getDependents() != null) {
            // Set all to ISCREATED for create operation
            request.getDependents().forEach(dto -> dto.setActionType(ACTION_CREATED));
            saveDependentDetails(entity.getTransactionPoid(), request.getDependents(), docId);
        }
        if (request.getEmergencyContacts() != null) {
            request.getEmergencyContacts().forEach(dto -> dto.setActionType(ACTION_CREATED));
            saveEmergencyContactDetails(entity.getTransactionPoid(), request.getEmergencyContacts(), docId);
        }
        if (request.getNominees() != null) {
            request.getNominees().forEach(dto -> dto.setActionType(ACTION_CREATED));
            saveNomineeDetails(entity.getTransactionPoid(), request.getNominees(), docId);
        }
        if (request.getPolicies() != null) {
            request.getPolicies().forEach(dto -> dto.setActionType(ACTION_CREATED));
            savePolicyDetails(entity.getTransactionPoid(), request.getPolicies(), docId);
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), entity.getTransactionPoid().toString());

        return mapToResponseDto(entity);
    }

    @Override
    public PersonalDataSheetResponseDto getById(Long transactionPoid) {
        log.info(LOG_FETCH_MSG, transactionPoid);

        HrPersonalDataHdr entity = repository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND + transactionPoid));

        return mapToResponseDto(entity);
    }

    @Override
    @Transactional
    public PersonalDataSheetResponseDto update(Long transactionPoid, PersonalDataSheetRequestDto request) {
        log.info(LOG_UPDATE_MSG, transactionPoid);

        HrPersonalDataHdr entity = repository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND + transactionPoid));

        validator.validateRequest(request);
        updateEntity(entity, request);
        entity.setLastmodifiedBy(UserContext.getUserId());

        entity = repository.save(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED
                , UserContext.getDocumentId(), entity.getTransactionPoid().toString());

        return mapToResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info(LOG_DELETE_MSG, transactionPoid);

        HrPersonalDataHdr entity = repository.findByTransactionPoidAndNotDeleted(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND + transactionPoid));

        documentDeleteService.deleteDocument(
                transactionPoid,
                TABLE_NAME,
                TRANSACTION_POID_FIELD,
                deleteReasonDto,
                entity.getTransactionDate()
        );
    }

    @Override
    public Map<String, Object> list(String documentId, FilterRequestDto filters, Pageable pageable) {
        log.info(LOG_LIST_MSG);
        
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveFilters(filters);

        RawSearchResult raw = documentSearchService.search(documentId, filterList, operator, pageable, isDeleted,
                EMPLOYEE_NAME_PASSPORT,
                TRANSACTION_POID_FIELD);
        
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public Map<String, Object> getLoginUserEmployee() {
        log.info(LOG_USER_EMPLOYEE_MSG, UserContext.getUserId());
        
        return procedureRepository.getLoginUserEmployeeId(
                UserContext.getUserPoid()
        );
    }

    @Override
    public List<Map<String, Object>> loadUserPolicies(Long employeePoid) {
        log.info(LOG_USER_POLICIES_MSG, employeePoid);
        
        // For new documents, use null to trigger the initial data load path from HR_USER_POLICIES_DOC_MASTER
        // The stored procedure checks: IF (p_doc_key_poid LIKE '%-999%' OR p_doc_key_poid IS NULL)
        Long docKeyPoid = null; // This will trigger the initial data load path
        
        return procedureRepository.loadUserPolicies(
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid(),
                UserContext.getUserPoid().toString(),
                UserContext.getDocumentId(), // docId for Personal Data Sheet
                docKeyPoid
        );
    }

    private HrPersonalDataHdr mapToEntity(PersonalDataSheetRequestDto request) {
        HrPersonalDataHdr entity = new HrPersonalDataHdr();
        
        entity.setEmployeePoid(request.getEmployeePoid());
        entity.setEmployeeNamePassport(request.getEmployeeNamePassport());
        entity.setEmployeeNameCpr(request.getEmployeeNameCpr());
        entity.setResidentStatus(request.getResidentStatus());
        
        // Address fields
        entity.setCurrentFlat(request.getCurrentFlat());
        entity.setCurrentBldg(request.getCurrentBldg());
        entity.setCurrentRoad(request.getCurrentRoad());
        entity.setCurrentBlock(request.getCurrentBlock());
        entity.setCurrentArea(request.getCurrentArea());
        entity.setCurrentPoBox(request.getCurrentPoBox());
        entity.setCurrentHomeTel(request.getCurrentHomeTel());
        entity.setCurrentMobile(request.getCurrentMobile());
        entity.setPersonalEmail(request.getPersonalEmail());
        entity.setPermanentAddress(request.getPermanentAddress());
        entity.setPostalAddress(request.getPostalAddress());
        entity.setRemarks(request.getRemarks());

        // Map child entities with action type handling (transactionPoid will be set after save)
        // For now, we'll set these after the entity is saved
        return entity;
    }

    private void updateEntity(HrPersonalDataHdr entity, PersonalDataSheetRequestDto request) {
        entity.setEmployeeNamePassport(request.getEmployeeNamePassport());
        entity.setEmployeeNameCpr(request.getEmployeeNameCpr());
        entity.setResidentStatus(request.getResidentStatus());
        
        // Address fields
        entity.setCurrentFlat(request.getCurrentFlat());
        entity.setCurrentBldg(request.getCurrentBldg());
        entity.setCurrentRoad(request.getCurrentRoad());
        entity.setCurrentBlock(request.getCurrentBlock());
        entity.setCurrentArea(request.getCurrentArea());
        entity.setCurrentPoBox(request.getCurrentPoBox());
        entity.setCurrentHomeTel(request.getCurrentHomeTel());
        entity.setCurrentMobile(request.getCurrentMobile());
        entity.setPersonalEmail(request.getPersonalEmail());
        entity.setPermanentAddress(request.getPermanentAddress());
        entity.setPostalAddress(request.getPostalAddress());
        entity.setRemarks(request.getRemarks());

        // Update child entities
        String docId = DOC_ID_DEFAULT;
        if (request.getDependents() != null) {
            saveDependentDetails(entity.getTransactionPoid(), request.getDependents(), docId);
        }
        if (request.getEmergencyContacts() != null) {
            saveEmergencyContactDetails(entity.getTransactionPoid(), request.getEmergencyContacts(), docId);
        }
        if (request.getNominees() != null) {
            saveNomineeDetails(entity.getTransactionPoid(), request.getNominees(), docId);
        }
        if (request.getPolicies() != null) {
            savePolicyDetails(entity.getTransactionPoid(), request.getPolicies(), docId);
        }
    }

    private void saveDependentDetails(Long transactionPoid, List<PersonalDataSheetRequestDto.DependentDto> dependentDtos, String docId) {
        List<HrPersonalDataDependent> toSave = new ArrayList<>();
        List<HrPersonalDataDependent> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<HrPersonalDataDependent>> logRequests = new ArrayList<>();

        List<HrPersonalDataDependent> existingList = dependentRepository.findByTransactionPoid(transactionPoid);
        Map<Long, HrPersonalDataDependent> existingMap = existingList.stream()
                .collect(Collectors.toMap(HrPersonalDataDependent::getDetRowId, d -> d));

        Long[] maxDetRowId = { existingList.stream()
                .map(HrPersonalDataDependent::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L) };

        for (PersonalDataSheetRequestDto.DependentDto dto : dependentDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : ACTION_CREATED;
            processDependentAction(action, dto, transactionPoid, existingMap, maxDetRowId, toSave, toUpdate, toDelete, logRequests, docId);
        }

        saveDependentBatch(toSave, toUpdate, toDelete, existingList, logRequests, transactionPoid, docId);
    }

    private void processDependentAction(String action, PersonalDataSheetRequestDto.DependentDto dto, Long transactionPoid,
                                      Map<Long, HrPersonalDataDependent> existingMap, Long[] maxDetRowId,
                                      List<HrPersonalDataDependent> toSave, List<HrPersonalDataDependent> toUpdate,
                                      List<Long> toDelete, List<LogRequestDto<HrPersonalDataDependent>> logRequests, String docId) {
        switch (action) {
            case ACTION_CREATED:
                Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : ++maxDetRowId[0];
                HrPersonalDataDependent newDependent = mapDependentDtoToEntity(dto, transactionPoid);
                newDependent.setDetRowId(detRowId);
                toSave.add(newDependent);
                break;
            case ACTION_UPDATED:
                HrPersonalDataDependent existing = existingMap.get(dto.getDetRowId());
                if (existing == null) {
                    throw new ResourceNotFoundException(DEPENDENT_DETAIL, DET_ROW_ID_FIELD, dto.getDetRowId());
                }
                HrPersonalDataDependent oldDependent = new HrPersonalDataDependent();
                BeanUtils.copyProperties(existing, oldDependent);
                updateDependentEntity(existing, dto);
                toUpdate.add(existing);
                String logDetail = String.format(KEY_ID_FORMAT, transactionPoid, dto.getDetRowId());
                logRequests.add(new LogRequestDto<>(oldDependent, existing, HrPersonalDataDependent.class, docId, transactionPoid.toString(), logDetail));
                break;
            case ACTION_DELETED:
                toDelete.add(dto.getDetRowId());
                loggingService.logDelete(dto, docId, transactionPoid.toString());
                break;
            default:
                log.warn(UNKNOWN_ACTION_LOG, action, "dependent", dto.getDetRowId());
                break;
        }
    }

    private void saveDependentBatch(List<HrPersonalDataDependent> toSave, List<HrPersonalDataDependent> toUpdate,
                                  List<Long> toDelete, List<HrPersonalDataDependent> existingList,
                                  List<LogRequestDto<HrPersonalDataDependent>> logRequests, Long transactionPoid, String docId) {
        if (!toSave.isEmpty()) {
            List<HrPersonalDataDependent> saved = dependentRepository.saveAll(toSave);
            saved.forEach(dependent -> {
                String logDetail = String.format(LOG_DETAIL_DEPENDENT, dependent.getDetRowId());
                loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            dependentRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            existingList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(dependentRepository::delete);
        }
    }

    private void saveEmergencyContactDetails(Long transactionPoid, List<PersonalDataSheetRequestDto.EmergencyContactDto> emergencyDtos, String docId) {
        List<HrPersonalDataEmergency> toSave = new ArrayList<>();
        List<HrPersonalDataEmergency> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<HrPersonalDataEmergency>> logRequests = new ArrayList<>();

        List<HrPersonalDataEmergency> existingList = emergencyRepository.findByTransactionPoid(transactionPoid);
        Map<Long, HrPersonalDataEmergency> existingMap = existingList.stream()
                .collect(Collectors.toMap(HrPersonalDataEmergency::getDetRowId, d -> d));

        Long[] maxDetRowId = { existingList.stream()
                .map(HrPersonalDataEmergency::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L) };

        for (PersonalDataSheetRequestDto.EmergencyContactDto dto : emergencyDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : ACTION_CREATED;
            processEmergencyAction(action, dto, transactionPoid, existingMap, maxDetRowId, toSave, toUpdate, toDelete, logRequests, docId);
        }

        saveEmergencyBatch(toSave, toUpdate, toDelete, existingList, logRequests, transactionPoid, docId);
    }

    private void processEmergencyAction(String action, PersonalDataSheetRequestDto.EmergencyContactDto dto, Long transactionPoid,
                                      Map<Long, HrPersonalDataEmergency> existingMap, Long[] maxDetRowId,
                                      List<HrPersonalDataEmergency> toSave, List<HrPersonalDataEmergency> toUpdate,
                                      List<Long> toDelete, List<LogRequestDto<HrPersonalDataEmergency>> logRequests, String docId) {
        switch (action) {
            case ACTION_CREATED:
                Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : ++maxDetRowId[0];
                HrPersonalDataEmergency newEmergency = mapEmergencyContactDtoToEntity(dto, transactionPoid);
                newEmergency.setDetRowId(detRowId);
                toSave.add(newEmergency);
                break;
            case ACTION_UPDATED:
                HrPersonalDataEmergency existing = existingMap.get(dto.getDetRowId());
                if (existing == null) {
                    throw new ResourceNotFoundException(EMERGENCY_CONTACT_DETAIL, DET_ROW_ID_FIELD, dto.getDetRowId());
                }
                HrPersonalDataEmergency oldEmergency = new HrPersonalDataEmergency();
                BeanUtils.copyProperties(existing, oldEmergency);
                updateEmergencyContactEntity(existing, dto);
                toUpdate.add(existing);
                String logDetail = String.format(KEY_ID_FORMAT, transactionPoid, dto.getDetRowId());
                logRequests.add(new LogRequestDto<>(oldEmergency, existing, HrPersonalDataEmergency.class, docId, transactionPoid.toString(), logDetail));
                break;
            case ACTION_DELETED:
                toDelete.add(dto.getDetRowId());
                loggingService.logDelete(dto, docId, transactionPoid.toString());
                break;
            default:
                log.warn(UNKNOWN_ACTION_LOG, action, "emergency contact", dto.getDetRowId());
                break;
        }
    }

    private void saveEmergencyBatch(List<HrPersonalDataEmergency> toSave, List<HrPersonalDataEmergency> toUpdate,
                                  List<Long> toDelete, List<HrPersonalDataEmergency> existingList,
                                  List<LogRequestDto<HrPersonalDataEmergency>> logRequests, Long transactionPoid, String docId) {
        if (!toSave.isEmpty()) {
            List<HrPersonalDataEmergency> saved = emergencyRepository.saveAll(toSave);
            saved.forEach(emergency -> {
                String logDetail = String.format(LOG_DETAIL_EMERGENCY, emergency.getDetRowId());
                loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            emergencyRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            existingList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(emergencyRepository::delete);
        }
    }

    private void saveNomineeDetails(Long transactionPoid, List<PersonalDataSheetRequestDto.NomineeDto> nomineeDtos, String docId) {
        List<HrPersonalDataNominee> toSave = new ArrayList<>();
        List<HrPersonalDataNominee> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<HrPersonalDataNominee>> logRequests = new ArrayList<>();

        List<HrPersonalDataNominee> existingList = nomineeRepository.findByTransactionPoid(transactionPoid);
        Map<Long, HrPersonalDataNominee> existingMap = existingList.stream()
                .collect(Collectors.toMap(HrPersonalDataNominee::getDetRowId, d -> d));

        Long[] maxDetRowId = { existingList.stream()
                .map(HrPersonalDataNominee::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L) };

        for (PersonalDataSheetRequestDto.NomineeDto dto : nomineeDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : ACTION_CREATED;
            processNomineeAction(action, dto, transactionPoid, existingMap, maxDetRowId, toSave, toUpdate, toDelete, logRequests, docId);
        }

        saveNomineeBatch(toSave, toUpdate, toDelete, existingList, logRequests, transactionPoid, docId);
    }

    private void processNomineeAction(String action, PersonalDataSheetRequestDto.NomineeDto dto, Long transactionPoid,
                                    Map<Long, HrPersonalDataNominee> existingMap, Long[] maxDetRowId,
                                    List<HrPersonalDataNominee> toSave, List<HrPersonalDataNominee> toUpdate,
                                    List<Long> toDelete, List<LogRequestDto<HrPersonalDataNominee>> logRequests, String docId) {
        switch (action) {
            case ACTION_CREATED:
                Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : ++maxDetRowId[0];
                HrPersonalDataNominee newNominee = mapNomineeDtoToEntity(dto, transactionPoid);
                newNominee.setDetRowId(detRowId);
                toSave.add(newNominee);
                break;
            case ACTION_UPDATED:
                HrPersonalDataNominee existing = existingMap.get(dto.getDetRowId());
                if (existing == null) {
                    throw new ResourceNotFoundException(NOMINEE_DETAIL, DET_ROW_ID_FIELD, dto.getDetRowId());
                }
                HrPersonalDataNominee oldNominee = new HrPersonalDataNominee();
                BeanUtils.copyProperties(existing, oldNominee);
                updateNomineeEntity(existing, dto);
                toUpdate.add(existing);
                String logDetail = String.format(KEY_ID_FORMAT, transactionPoid, dto.getDetRowId());
                logRequests.add(new LogRequestDto<>(oldNominee, existing, HrPersonalDataNominee.class, docId, transactionPoid.toString(), logDetail));
                break;
            case ACTION_DELETED:
                toDelete.add(dto.getDetRowId());
                loggingService.logDelete(dto, docId, transactionPoid.toString());
                break;
            default:
                log.warn(UNKNOWN_ACTION_LOG, action, "nominee", dto.getDetRowId());
                break;
        }
    }

    private void saveNomineeBatch(List<HrPersonalDataNominee> toSave, List<HrPersonalDataNominee> toUpdate,
                                List<Long> toDelete, List<HrPersonalDataNominee> existingList,
                                List<LogRequestDto<HrPersonalDataNominee>> logRequests, Long transactionPoid, String docId) {
        if (!toSave.isEmpty()) {
            List<HrPersonalDataNominee> saved = nomineeRepository.saveAll(toSave);
            saved.forEach(nominee -> {
                String logDetail = String.format(LOG_DETAIL_NOMINEE, nominee.getDetRowId());
                loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            nomineeRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            existingList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(nomineeRepository::delete);
        }
    }

    private void savePolicyDetails(Long transactionPoid, List<PersonalDataSheetRequestDto.PolicyDto> policyDtos, String docId) {
        List<HrPersonalDataPolicies> toSave = new ArrayList<>();
        List<HrPersonalDataPolicies> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<HrPersonalDataPolicies>> logRequests = new ArrayList<>();

        List<HrPersonalDataPolicies> existingList = policiesRepository.findByTransactionPoid(transactionPoid);
        Map<Long, HrPersonalDataPolicies> existingMap = existingList.stream()
                .collect(Collectors.toMap(HrPersonalDataPolicies::getDetRowId, d -> d));

        Long[] maxDetRowId = { existingList.stream()
                .map(HrPersonalDataPolicies::getDetRowId)
                .max(Long::compareTo)
                .orElse(0L) };

        for (PersonalDataSheetRequestDto.PolicyDto dto : policyDtos) {
            String action = dto.getActionType() != null ? dto.getActionType().toUpperCase() : ACTION_CREATED;
            processPolicyAction(action, dto, transactionPoid, existingMap, maxDetRowId, toSave, toUpdate, toDelete, logRequests, docId);
        }

        savePolicyBatch(toSave, toUpdate, toDelete, existingList, logRequests, transactionPoid, docId);
    }

    private void processPolicyAction(String action, PersonalDataSheetRequestDto.PolicyDto dto, Long transactionPoid,
                                   Map<Long, HrPersonalDataPolicies> existingMap, Long[] maxDetRowId,
                                   List<HrPersonalDataPolicies> toSave, List<HrPersonalDataPolicies> toUpdate,
                                   List<Long> toDelete, List<LogRequestDto<HrPersonalDataPolicies>> logRequests, String docId) {
        switch (action) {
            case ACTION_CREATED:
                Long detRowId = dto.getDetRowId() != null ? dto.getDetRowId() : ++maxDetRowId[0];
                HrPersonalDataPolicies newPolicy = mapPolicyDtoToEntity(dto, transactionPoid);
                newPolicy.setDetRowId(detRowId);
                toSave.add(newPolicy);
                break;
            case ACTION_UPDATED:
                HrPersonalDataPolicies existing = existingMap.get(dto.getDetRowId());
                if (existing == null) {
                    throw new ResourceNotFoundException(POLICY_DETAIL, DET_ROW_ID_FIELD, dto.getDetRowId());
                }
                HrPersonalDataPolicies oldPolicy = new HrPersonalDataPolicies();
                BeanUtils.copyProperties(existing, oldPolicy);
                updatePolicyEntity(existing, dto);
                toUpdate.add(existing);
                String logDetail = String.format(KEY_ID_FORMAT, transactionPoid, dto.getDetRowId());
                logRequests.add(new LogRequestDto<>(oldPolicy, existing, HrPersonalDataPolicies.class, docId, transactionPoid.toString(), logDetail));
                break;
            case ACTION_DELETED:
                toDelete.add(dto.getDetRowId());
                loggingService.logDelete(dto, docId, transactionPoid.toString());
                break;
            default:
                log.warn(UNKNOWN_ACTION_LOG, action, "policy", dto.getDetRowId());
                break;
        }
    }

    private void savePolicyBatch(List<HrPersonalDataPolicies> toSave, List<HrPersonalDataPolicies> toUpdate,
                               List<Long> toDelete, List<HrPersonalDataPolicies> existingList,
                               List<LogRequestDto<HrPersonalDataPolicies>> logRequests, Long transactionPoid, String docId) {
        if (!toSave.isEmpty()) {
            List<HrPersonalDataPolicies> saved = policiesRepository.saveAll(toSave);
            saved.forEach(policy -> {
                String logDetail = String.format(LOG_DETAIL_POLICY, policy.getDetRowId());
                loggingService.createLogSummaryEntry(docId, transactionPoid.toString(), logDetail);
            });
        }
        if (!toUpdate.isEmpty()) {
            policiesRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
        if (!toDelete.isEmpty()) {
            existingList.stream()
                    .filter(d -> toDelete.contains(d.getDetRowId()))
                    .forEach(policiesRepository::delete);
        }
    }


    private void updateDependentEntity(HrPersonalDataDependent entity, PersonalDataSheetRequestDto.DependentDto dto) {
        entity.setNamePassport(dto.getNamePassport());
        entity.setRelation(dto.getRelation());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setNationality(dto.getNationality());
        entity.setPassportNo(dto.getPassportNo());
        entity.setPpExpiryDate(dto.getPpExpiryDate());
        entity.setCprNo(dto.getCprNo());
        entity.setCprExpiry(dto.getCprExpiry());
        entity.setRpExpiry(dto.getRpExpiry());
        entity.setVisaSponsor(dto.getVisaSponsor());
        entity.setMobile(dto.getMobile());
        entity.setTelephone(dto.getTelephone());
        entity.setRemarks(dto.getRemarks());
        entity.setLastmodifiedBy(UserContext.getUserId());
    }
    
    private void updateEmergencyContactEntity(HrPersonalDataEmergency entity, PersonalDataSheetRequestDto.EmergencyContactDto dto) {
        entity.setName(dto.getName());
        entity.setRelation(dto.getRelation());
        entity.setMobile(dto.getMobile());
        entity.setHomeTel(dto.getHomeTel());
        entity.setEmail(dto.getEmail());
        entity.setCountry(dto.getCountry());
        entity.setRemarks(dto.getRemarks());
        entity.setLastmodifiedBy(UserContext.getUserId());
    }
    
    private void updateNomineeEntity(HrPersonalDataNominee entity, PersonalDataSheetRequestDto.NomineeDto dto) {
        entity.setNomineeType(dto.getNomineeType());
        entity.setPercentage(dto.getPercentage() != null ? BigDecimal.valueOf(dto.getPercentage()) : null);
        entity.setNomineeName(dto.getNomineeName());
        entity.setRelation(dto.getRelation());
        entity.setMobile(dto.getMobile());
        entity.setAddress(dto.getAddress());
        entity.setBankDetails(dto.getBankDetails());
        entity.setRemarks(dto.getRemarks());
        entity.setLastmodifiedBy(UserContext.getUserId());
    }
    
    private void updatePolicyEntity(HrPersonalDataPolicies entity, PersonalDataSheetRequestDto.PolicyDto dto) {
        entity.setDocPoid(dto.getDocPoid());
        entity.setDocName(dto.getDocName());
        entity.setDrilldownLinkInfo(dto.getDrilldownLinkInfo());
        entity.setPolicyAccepted(dto.getPolicyAccepted());
        entity.setPolicyAcceptedOn(dto.getPolicyAcceptedOn());
        entity.setLastmodifiedBy(UserContext.getUserId());
    }



    private HrPersonalDataDependent mapDependentDtoToEntity(PersonalDataSheetRequestDto.DependentDto dto, Long transactionPoid) {
        HrPersonalDataDependent dependent = new HrPersonalDataDependent();
        dependent.setTransactionPoid(transactionPoid);
        dependent.setNamePassport(dto.getNamePassport());
        dependent.setRelation(dto.getRelation());
        dependent.setDateOfBirth(dto.getDateOfBirth());
        dependent.setNationality(dto.getNationality());
        dependent.setPassportNo(dto.getPassportNo());
        dependent.setPpExpiryDate(dto.getPpExpiryDate());
        dependent.setCprNo(dto.getCprNo());
        dependent.setCprExpiry(dto.getCprExpiry());
        dependent.setRpExpiry(dto.getRpExpiry());
        dependent.setVisaSponsor(dto.getVisaSponsor());
        dependent.setMobile(dto.getMobile());
        dependent.setTelephone(dto.getTelephone());
        dependent.setRemarks(dto.getRemarks());
        dependent.setCreatedBy(UserContext.getUserId());
        return dependent;
    }

    private HrPersonalDataEmergency mapEmergencyContactDtoToEntity(PersonalDataSheetRequestDto.EmergencyContactDto dto, Long transactionPoid) {
        HrPersonalDataEmergency emergency = new HrPersonalDataEmergency();
        emergency.setTransactionPoid(transactionPoid);
        emergency.setName(dto.getName());
        emergency.setRelation(dto.getRelation());
        emergency.setMobile(dto.getMobile());
        emergency.setHomeTel(dto.getHomeTel());
        emergency.setEmail(dto.getEmail());
        emergency.setCountry(dto.getCountry());
        emergency.setRemarks(dto.getRemarks());
        emergency.setCreatedBy(UserContext.getUserId());
        return emergency;
    }

    private HrPersonalDataNominee mapNomineeDtoToEntity(PersonalDataSheetRequestDto.NomineeDto dto, Long transactionPoid) {
        HrPersonalDataNominee nominee = new HrPersonalDataNominee();
        nominee.setTransactionPoid(transactionPoid);
        nominee.setNomineeType(dto.getNomineeType());
        nominee.setPercentage(dto.getPercentage() != null ? BigDecimal.valueOf(dto.getPercentage()) : null);
        nominee.setNomineeName(dto.getNomineeName());
        nominee.setRelation(dto.getRelation());
        nominee.setMobile(dto.getMobile());
        nominee.setAddress(dto.getAddress());
        nominee.setBankDetails(dto.getBankDetails());
        nominee.setRemarks(dto.getRemarks());
        nominee.setCreatedBy(UserContext.getUserId());
        return nominee;
    }

    private HrPersonalDataPolicies mapPolicyDtoToEntity(PersonalDataSheetRequestDto.PolicyDto dto, Long transactionPoid) {
        HrPersonalDataPolicies policy = new HrPersonalDataPolicies();
        policy.setTransactionPoid(transactionPoid);
        policy.setDocPoid(dto.getDocPoid());
        policy.setDocName(dto.getDocName());
        policy.setDrilldownLinkInfo(dto.getDrilldownLinkInfo());
        policy.setPolicyAccepted(dto.getPolicyAccepted());
        policy.setPolicyAcceptedOn(dto.getPolicyAcceptedOn());
        policy.setCreatedBy(UserContext.getUserId());
        return policy;
    }

    private PersonalDataSheetResponseDto mapToResponseDto(HrPersonalDataHdr entity) {
        PersonalDataSheetResponseDto response = new PersonalDataSheetResponseDto();
        
        response.setTransactionPoid(entity.getTransactionPoid());
        response.setGroupPoid(entity.getGroupPoid());
        response.setCompanyPoid(entity.getCompanyPoid());
        response.setTransactionDate(entity.getTransactionDate());
        response.setDocRef(entity.getDocRef());
        response.setEmployeePoid(entity.getEmployeePoid());
        response.setEmployeeNamePassport(entity.getEmployeeNamePassport());
        response.setEmployeeNameCpr(entity.getEmployeeNameCpr());
        response.setResidentStatus(entity.getResidentStatus());
        
        // Address fields
        response.setCurrentFlat(entity.getCurrentFlat());
        response.setCurrentBldg(entity.getCurrentBldg());
        response.setCurrentRoad(entity.getCurrentRoad());
        response.setCurrentBlock(entity.getCurrentBlock());
        response.setCurrentArea(entity.getCurrentArea());
        response.setCurrentPoBox(entity.getCurrentPoBox());
        response.setCurrentHomeTel(entity.getCurrentHomeTel());
        response.setCurrentMobile(entity.getCurrentMobile());
        response.setPersonalEmail(entity.getPersonalEmail());
        response.setPermanentAddress(entity.getPermanentAddress());
        response.setPostalAddress(entity.getPostalAddress());
        
        // Read-only fields
        response.setPassportNo(entity.getPassportNo());
        response.setPassportExpiryDt(entity.getPassportExpiryDt());
        response.setCprNo(entity.getCprNo());
        response.setCprExpiryDt(entity.getCprExpiryDt());
        
        response.setRemarks(entity.getRemarks());
        response.setStatus(entity.getStatus());
        response.setDeleted(entity.getDeleted());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastmodifiedBy(entity.getLastmodifiedBy());
        response.setLastmodifiedDate(entity.getLastmodifiedDate());

        // Load child entities from individual repositories
        List<HrPersonalDataDependent> dependents = dependentRepository.findByTransactionPoid(entity.getTransactionPoid());
        if (!dependents.isEmpty()) {
            response.setDependents(dependents.stream().map(this::mapDependentToDto).toList());
        }
        
        List<HrPersonalDataEmergency> emergencyContacts = emergencyRepository.findByTransactionPoid(entity.getTransactionPoid());
        if (!emergencyContacts.isEmpty()) {
            response.setEmergencyContacts(emergencyContacts.stream().map(this::mapEmergencyToDto).toList());
        }
        
        List<HrPersonalDataNominee> nominees = nomineeRepository.findByTransactionPoid(entity.getTransactionPoid());
        if (!nominees.isEmpty()) {
            response.setNominees(nominees.stream().map(this::mapNomineeToDto).toList());
        }
        
        List<HrPersonalDataPolicies> policies = policiesRepository.findByTransactionPoid(entity.getTransactionPoid());
        if (!policies.isEmpty()) {
            response.setPolicies(policies.stream().map(this::mapPolicyToDto).toList());
        }

        return response;
    }

    private PersonalDataSheetResponseDto.DependentResponseDto mapDependentToDto(HrPersonalDataDependent entity) {
        PersonalDataSheetResponseDto.DependentResponseDto dto = new PersonalDataSheetResponseDto.DependentResponseDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setNamePassport(entity.getNamePassport());
        dto.setRelation(entity.getRelation());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setNationality(entity.getNationality());
        dto.setPassportNo(entity.getPassportNo());
        dto.setPpExpiryDate(entity.getPpExpiryDate());
        dto.setCprNo(entity.getCprNo());
        dto.setCprExpiry(entity.getCprExpiry());
        dto.setRpExpiry(entity.getRpExpiry());
        dto.setVisaSponsor(entity.getVisaSponsor());
        dto.setMobile(entity.getMobile());
        dto.setTelephone(entity.getTelephone());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }

    private PersonalDataSheetResponseDto.EmergencyContactResponseDto mapEmergencyToDto(HrPersonalDataEmergency entity) {
        PersonalDataSheetResponseDto.EmergencyContactResponseDto dto = new PersonalDataSheetResponseDto.EmergencyContactResponseDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setName(entity.getName());
        dto.setRelation(entity.getRelation());
        dto.setMobile(entity.getMobile());
        dto.setHomeTel(entity.getHomeTel());
        dto.setEmail(entity.getEmail());
        dto.setCountry(entity.getCountry());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }

    private PersonalDataSheetResponseDto.NomineeResponseDto mapNomineeToDto(HrPersonalDataNominee entity) {
        PersonalDataSheetResponseDto.NomineeResponseDto dto = new PersonalDataSheetResponseDto.NomineeResponseDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setNomineeType(entity.getNomineeType());
        dto.setPercentage(entity.getPercentage());
        dto.setNomineeName(entity.getNomineeName());
        dto.setRelation(entity.getRelation());
        dto.setMobile(entity.getMobile());
        dto.setAddress(entity.getAddress());
        dto.setBankDetails(entity.getBankDetails());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }

    private PersonalDataSheetResponseDto.PolicyResponseDto mapPolicyToDto(HrPersonalDataPolicies entity) {
        PersonalDataSheetResponseDto.PolicyResponseDto dto = new PersonalDataSheetResponseDto.PolicyResponseDto();
        dto.setDetRowId(entity.getDetRowId());
        dto.setDocPoid(entity.getDocPoid());
        dto.setDocName(entity.getDocName());
        dto.setDrilldownLinkInfo(entity.getDrilldownLinkInfo());
        dto.setPolicyAccepted(entity.getPolicyAccepted());
        dto.setPolicyAcceptedOn(entity.getPolicyAcceptedOn());
        return dto;
    }
}