package com.asg.hr.employeetraining.service.impl;

import com.asg.common.lib.dto.*;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.employeetraining.dto.EmployeeTrainingDetailRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingRequest;
import com.asg.hr.employeetraining.dto.EmployeeTrainingResponse;
import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailId;
import com.asg.hr.employeetraining.entity.EmployeeTrainingDetailEntity;
import com.asg.hr.employeetraining.entity.EmployeeTrainingHeaderEntity;
import com.asg.hr.employeetraining.repository.EmployeeTrainingDetailRepository;
import com.asg.hr.employeetraining.repository.EmployeeTrainingHeaderRepository;
import com.asg.hr.employeetraining.repository.EmployeeTrainingProcedureRepository;
import com.asg.hr.employeetraining.service.EmployeeTrainingService;
import com.asg.hr.employeetraining.util.EmployeeTrainingConstants;
import com.asg.hr.employeetraining.util.EmployeeTrainingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

import static com.asg.common.lib.utility.ASGHelperUtils.getCompanyId;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeTrainingServiceImpl implements EmployeeTrainingService {

    private final EmployeeTrainingHeaderRepository headerRepository;
    private final EmployeeTrainingDetailRepository detailRepository;
    private final EmployeeTrainingProcedureRepository procedureRepository;
    private final EmployeeTrainingMapper mapper;
    private final DocumentSearchService documentSearchService;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final PrintService printService;
    private final DataSource dataSource;

    @Override
    @Transactional(readOnly = true)
    /**
     * Lists employee training records using document-search filters and pagination.
     *
     * @param docId document master id used by document-search
     * @param filterRequest dynamic search filters (may be null/empty)
     * @param pageable pagination and sorting information
     * @return a map containing paginated records and metadata
     */
    public Map<String, Object> listTrainings(String docId, FilterRequestDto filterRequest, Pageable pageable) {

        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filters = documentSearchService.resolveFilters(filterRequest);

        RawSearchResult raw = documentSearchService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "COURSE_NAME",
                "TRANSACTION_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Fetches a single employee training transaction (header + detail rows) and enriches LOV fields.
     *
     * @param transactionPoid header transaction poid
     * @return populated response DTO containing header and details
     */
    public EmployeeTrainingResponse getTrainingById(Long transactionPoid) {
        EmployeeTrainingHeaderEntity header = fetchActiveTraining(transactionPoid);
        List<EmployeeTrainingDetailEntity> details = detailRepository.findByIdTransactionPoidOrderByIdDetRowIdAsc(transactionPoid);
        EmployeeTrainingResponse response = mapper.toResponse(header, details);
        appendLovData(response);

        return response;
    }

    @Override
    @Transactional
    /**
     * Creates a new employee training transaction including its detail rows.
     * <p>
     * Performs request normalization/validation, duplicate checks, persists header and details, and writes logs.
     * </p>
     *
     * @param request create request payload
     * @return created response DTO
     */
    public EmployeeTrainingResponse createTraining(EmployeeTrainingRequest request) {
        normalizeAndValidateRequest(request);
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = getCompanyId();

        validateHeaderDuplicateForCreate(request);

        EmployeeTrainingHeaderEntity header = mapper.toHeaderEntity(request, companyPoid, groupPoid);
        EmployeeTrainingHeaderEntity savedHeader = headerRepository.save(header);

        List<EmployeeTrainingDetailRequest> details = request.getDetails() == null ? List.of() : request.getDetails();
        List<EmployeeTrainingDetailEntity> detailEntities = mapper.toDetailEntities(
                savedHeader.getTransactionPoid(),
                details
        );
        if (!detailEntities.isEmpty()) {
            detailEntities = detailRepository.saveAll(detailEntities);
            String docId = UserContext.getDocumentId();
            String docKeyPoid = savedHeader.getTransactionPoid().toString();
            for (EmployeeTrainingDetailEntity entity : detailEntities) {
                String logDetail = String.format(
                        "Row Created on Employee Training Detail with detRowId: %s",
                        entity.getId().getDetRowId()
                );
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            }
        }

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.CREATED,
                UserContext.getDocumentId(),
                savedHeader.getTransactionPoid().toString()
        );

        return mapper.toResponse(savedHeader, detailEntities);
    }

    @Override
    @Transactional
    /**
     * Updates an existing employee training transaction including its detail rows.
     * <p>
     * Performs request normalization/validation, duplicate checks, reconciles detail rows based on actionType/detRowId,
     * persists changes, and writes change logs.
     * </p>
     *
     * @param transactionPoid header transaction poid to update
     * @param request update request payload
     * @return updated response DTO
     */
    public EmployeeTrainingResponse updateTraining(Long transactionPoid, EmployeeTrainingRequest request) {
        normalizeAndValidateRequest(request);
        EmployeeTrainingHeaderEntity header = fetchActiveTraining(transactionPoid);

        EmployeeTrainingHeaderEntity oldEntity = new EmployeeTrainingHeaderEntity();
        BeanUtils.copyProperties(header, oldEntity);

        mapper.updateHeaderEntity(header, request, getCompanyId(), UserContext.getGroupPoid());
        validateHeaderDuplicateForUpdate(header, transactionPoid);

        EmployeeTrainingHeaderEntity saved = headerRepository.save(header);
        List<EmployeeTrainingDetailEntity> detailEntities =
                updateEmployeeTrainingDetailsDiff(transactionPoid, request.getDetails());

        loggingService.logChanges(
                oldEntity,
                saved,
                EmployeeTrainingHeaderEntity.class,
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                LogDetailsEnum.MODIFIED,
                EmployeeTrainingConstants.KEY_FIELD
        );

        return mapper.toResponse(saved, detailEntities);
    }

    private List<EmployeeTrainingDetailEntity> updateEmployeeTrainingDetailsDiff(
            Long transactionPoid,
            List<EmployeeTrainingDetailRequest> detailRequests) {
        List<EmployeeTrainingDetailRequest> details =
                detailRequests == null ? List.of() : detailRequests;

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<EmployeeTrainingDetailEntity> existingDetails =
                detailRepository.findByIdTransactionPoidOrderByIdDetRowIdAsc(transactionPoid);

        ExistingDetailIndex existingIndex = indexExistingDetails(existingDetails);
        Map<Integer, EmployeeTrainingDetailEntity> existingByDetRowId = existingIndex.existingByDetRowId();
        int maxDetRowId = existingIndex.maxDetRowId();

        List<EmployeeTrainingDetailEntity> deletions = new ArrayList<>();
        List<EmployeeTrainingDetailEntity> toSave = new ArrayList<>();
        List<EmployeeTrainingDetailEntity> toUpdate = new ArrayList<>();
        List<LogRequestDto<EmployeeTrainingDetailEntity>> logRequests = new ArrayList<>();

        Set<Integer> deletedDetRowIds = new HashSet<>();

        for (EmployeeTrainingDetailRequest d : details) {
            if (d == null) continue;

            String action = resolveDetailActionType(d.getActionType(), d.getDetRowId());
            Integer detRowIdNorm = normalizeDetRowId(d.getDetRowId());

            switch (action) {
                case EmployeeTrainingConstants.ACTION_IS_CREATED: {
                    maxDetRowId = addCreatedDetail(transactionPoid, d, maxDetRowId, toSave);
                    break;
                }
                case EmployeeTrainingConstants.ACTION_IS_UPDATED: {
                    applyUpdatedDetail(transactionPoid, d, detRowIdNorm, existingByDetRowId, toUpdate, logRequests, docKeyPoid);
                    break;
                }
                case EmployeeTrainingConstants.ACTION_IS_DELETED: {
                    applyDeletedDetail(detRowIdNorm, existingByDetRowId, deletions, deletedDetRowIds);
                    break;
                }
                case EmployeeTrainingConstants.ACTION_NO_CHANGE:
                default:
                    break;
            }
        }

        persistDetailDeletions(deletions, docId, docKeyPoid);
        persistDetailCreations(toSave, docId, docKeyPoid);
        persistDetailUpdates(toUpdate, logRequests);

        return buildAllDetails(existingByDetRowId, deletedDetRowIds, toSave);
    }

    private record ExistingDetailIndex(Map<Integer, EmployeeTrainingDetailEntity> existingByDetRowId, int maxDetRowId) {
    }

    private ExistingDetailIndex indexExistingDetails(List<EmployeeTrainingDetailEntity> existingDetails) {
        Map<Integer, EmployeeTrainingDetailEntity> existingByDetRowId = new HashMap<>();
        int maxDetRowId = 0;
        if (existingDetails != null) {
            for (EmployeeTrainingDetailEntity e : existingDetails) {
                if (e == null || e.getId() == null || e.getId().getDetRowId() == null) continue;
                int detRowId = e.getId().getDetRowId();
                existingByDetRowId.put(detRowId, e);
                maxDetRowId = Math.max(maxDetRowId, detRowId);
            }
        }
        return new ExistingDetailIndex(existingByDetRowId, maxDetRowId);
    }

    private int addCreatedDetail(
            Long transactionPoid,
            EmployeeTrainingDetailRequest request,
            int maxDetRowId,
            List<EmployeeTrainingDetailEntity> toSave
    ) {
        int detRowIdToUse = maxDetRowId + 1;
        EmployeeTrainingDetailEntity entity = EmployeeTrainingDetailEntity.builder()
                .id(new EmployeeTrainingDetailId(transactionPoid, detRowIdToUse))
                .empPoid(request.getEmpPoid())
                .trainingStatus(trim(request.getTrainingStatus()))
                .completedOn(request.getCompletedOn())
                .otherRemarks(trim(request.getOtherRemarks()))
                .build();
        toSave.add(entity);
        return detRowIdToUse;
    }

    private void applyUpdatedDetail(
            Long transactionPoid,
            EmployeeTrainingDetailRequest request,
            Integer detRowIdNorm,
            Map<Integer, EmployeeTrainingDetailEntity> existingByDetRowId,
            List<EmployeeTrainingDetailEntity> toUpdate,
            List<LogRequestDto<EmployeeTrainingDetailEntity>> logRequests,
            String docKeyPoid
    ) {
        if (detRowIdNorm == null) {
            throw new ValidationException("detRowId is required for updating detail row");
        }
        EmployeeTrainingDetailEntity entity = existingByDetRowId.get(detRowIdNorm);
        if (entity == null) {
            throw new ResourceNotFoundException("EmployeeTrainingDetail", "detRowId", detRowIdNorm.toString());
        }

        EmployeeTrainingDetailEntity oldItem = new EmployeeTrainingDetailEntity();
        BeanUtils.copyProperties(entity, oldItem);

        entity.setEmpPoid(request.getEmpPoid());
        entity.setTrainingStatus(trim(request.getTrainingStatus()));
        entity.setCompletedOn(request.getCompletedOn());
        entity.setOtherRemarks(trim(request.getOtherRemarks()));

        toUpdate.add(entity);

        String logDetailForUpdate = String.format(
                "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s",
                transactionPoid,
                detRowIdNorm
        );
        logRequests.add(new LogRequestDto<>(oldItem, entity, EmployeeTrainingDetailEntity.class, UserContext.getDocumentId(), docKeyPoid, logDetailForUpdate));
    }

    private void applyDeletedDetail(
            Integer detRowIdNorm,
            Map<Integer, EmployeeTrainingDetailEntity> existingByDetRowId,
            List<EmployeeTrainingDetailEntity> deletions,
            Set<Integer> deletedDetRowIds
    ) {
        if (detRowIdNorm == null) {
            throw new ValidationException("detRowId is required for deleting detail row");
        }
        EmployeeTrainingDetailEntity entityToDelete = existingByDetRowId.get(detRowIdNorm);
        if (entityToDelete == null) {
            throw new ResourceNotFoundException("EmployeeTrainingDetail", "detRowId", detRowIdNorm.toString());
        }
        deletions.add(entityToDelete);
        deletedDetRowIds.add(detRowIdNorm);
    }

    private void persistDetailDeletions(List<EmployeeTrainingDetailEntity> deletions, String docId, String docKeyPoid) {
        if (deletions.isEmpty()) {
            return;
        }
        detailRepository.deleteAll(deletions);
        deletions.forEach(deleted -> loggingService.logDelete(deleted, docId, docKeyPoid));
    }

    private void persistDetailCreations(List<EmployeeTrainingDetailEntity> toSave, String docId, String docKeyPoid) {
        if (toSave.isEmpty()) {
            return;
        }
        List<EmployeeTrainingDetailEntity> savedItems = detailRepository.saveAll(toSave);
        savedItems.forEach(created -> {
            String logDetail = String.format(
                    "Row Created on Employee Training Detail with detRowId: %s",
                    created.getId().getDetRowId()
            );
            loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
        });
    }

    private void persistDetailUpdates(
            List<EmployeeTrainingDetailEntity> toUpdate,
            List<LogRequestDto<EmployeeTrainingDetailEntity>> logRequests
    ) {
        if (toUpdate.isEmpty()) {
            return;
        }
        detailRepository.saveAll(toUpdate);
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }

    private List<EmployeeTrainingDetailEntity> buildAllDetails(
            Map<Integer, EmployeeTrainingDetailEntity> existingByDetRowId,
            Set<Integer> deletedDetRowIds,
            List<EmployeeTrainingDetailEntity> toSave
    ) {
        List<EmployeeTrainingDetailEntity> allItems = new ArrayList<>();
        for (EmployeeTrainingDetailEntity existing : existingByDetRowId.values()) {
            if (existing == null || existing.getId() == null || existing.getId().getDetRowId() == null) {
                continue;
            }

            int detRowId = existing.getId().getDetRowId();
            if (!deletedDetRowIds.contains(detRowId)) {
                allItems.add(existing);
            }
        }
        allItems.addAll(toSave);
        allItems.sort(Comparator.comparing(e -> e.getId().getDetRowId()));
        return allItems;
    }

    @Override
    @Transactional
    /**
     * Soft deletes an employee training transaction.
     *
     * @param transactionPoid header transaction poid
     * @param deleteReasonDto optional delete reason payload
     */
    public void deleteTraining(Long transactionPoid, DeleteReasonDto deleteReasonDto) {

        log.info("Deleting (soft) employee training with id: {}", transactionPoid);

        headerRepository.findById(transactionPoid).orElseThrow(() -> new ResourceNotFoundException(
                "Employee Training",
                "transactionPoid",
                transactionPoid
        ));

        documentDeleteService.deleteDocument(
                transactionPoid,
                EmployeeTrainingConstants.TABLE_NAME,
                EmployeeTrainingConstants.KEY_FIELD,
                deleteReasonDto,
                LocalDate.now()
        );
    }

    private void appendLovData(EmployeeTrainingResponse response) {
        Set<Long> employeeIds = new HashSet<>();
        Long headerEmployeePoid = parseLongSafely(response.getEmployeePoid());
        if (headerEmployeePoid != null) {
            employeeIds.add(headerEmployeePoid);
        }
        if (response.getDetails() != null) {
            response.getDetails().forEach(detail -> {
                if (detail.getEmpPoid() != null) {
                    employeeIds.add(detail.getEmpPoid());
                }
            });
        }

        List<LovGetListDto> employeeLov = procedureRepository.getEmployeeLovByIds(employeeIds.stream().toList());
        LovGetListDto trainingTypeDet = procedureRepository.getTrainingTypeByCode(response.getTrainingType());
        List<String> trainingStatuses = response.getDetails() == null
                ? List.of()
                : response.getDetails().stream()
                        .map(detail -> detail.getTrainingStatus())
                        .filter(status -> status != null && !status.isBlank())
                        .distinct()
                        .toList();
        List<LovGetListDto> trainingStatusLov = procedureRepository.getTrainingStatusByCodes(trainingStatuses);

        Map<Long, LovGetListDto> employeeByPoid = toPoidMap(employeeLov);
        Map<String, LovGetListDto> trainingStatusByCode = toCodeMap(trainingStatusLov);

        response.setEmployeeDet(employeeByPoid.get(parseLongSafely(response.getEmployeePoid())));
        response.setTrainingTypeDet(trainingTypeDet);

        if (response.getDetails() != null) {
            response.getDetails().forEach(detail -> {
                detail.setEmpDet(employeeByPoid.get(detail.getEmpPoid()));
                detail.setTrainingStatusDet(trainingStatusByCode.get(normalizeCode(detail.getTrainingStatus())));
            });
        }
    }

    private Map<Long, LovGetListDto> toPoidMap(List<LovGetListDto> lovList) {
        Map<Long, LovGetListDto> map = new HashMap<>();
        if (lovList != null) {
            lovList.forEach(item -> {
                if (item != null && item.getPoid() != null) {
                    map.put(item.getPoid(), item);
                }
            });
        }
        return map;
    }

    private Map<String, LovGetListDto> toCodeMap(List<LovGetListDto> lovList) {
        Map<String, LovGetListDto> map = new HashMap<>();
        if (lovList != null) {
            lovList.forEach(item -> {
                if (item != null && item.getCode() != null) {
                    map.put(normalizeCode(item.getCode()), item);
                }
            });
        }
        return map;
    }

    private Long parseLongSafely(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private EmployeeTrainingHeaderEntity fetchActiveTraining(Long transactionPoid) {
        return headerRepository.findByTransactionPoidAndDeletedNot(transactionPoid, EmployeeTrainingConstants.DELETED_YES)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Training", "transactionPoid", transactionPoid));
    }

    private void normalizeAndValidateRequest(EmployeeTrainingRequest request) {
        if (request.getPeriodFrom() != null && request.getPeriodTo() != null
                && request.getPeriodFrom().isAfter(request.getPeriodTo())) {
            throw new ValidationException("Period from cannot be later than period to");
        }

        if (request.getDurationDays() == null && request.getPeriodFrom() != null && request.getPeriodTo() != null) {
            long days = ChronoUnit.DAYS.between(request.getPeriodFrom(), request.getPeriodTo()) + 1;
            request.setDurationDays((int) Math.max(0, days));
        }

        if (request.getDetails() != null) {
            for (int i = 0; i < request.getDetails().size(); i++) {
                EmployeeTrainingDetailRequest detail = request.getDetails().get(i);
                validateDetailRow(detail, i + 1);
                validateDetailAction(detail, i + 1);
            }
        }

        if (request.getEmployeePoid() == null || request.getEmployeePoid().isBlank()) {
            throw new ValidationException("Employee is required");
        }
    }

    private void validateDetailAction(EmployeeTrainingDetailRequest detail, int rowNumber) {
        if (detail == null) return;
        String action = resolveDetailActionType(detail.getActionType(), detail.getDetRowId());
        Integer detRowIdNorm = normalizeDetRowId(detail.getDetRowId());

        // Guard against invalid updated/deleted operations without detRowId.
        if ((EmployeeTrainingConstants.ACTION_IS_UPDATED.equals(action)
                || EmployeeTrainingConstants.ACTION_IS_DELETED.equals(action))
                && detRowIdNorm == null) {
            throw new ValidationException("detRowId is required for " + action + " in row " + rowNumber);
        }

        switch (action) {
            case EmployeeTrainingConstants.ACTION_IS_UPDATED,
                    EmployeeTrainingConstants.ACTION_IS_DELETED,
                    EmployeeTrainingConstants.ACTION_IS_CREATED,
                    EmployeeTrainingConstants.ACTION_NO_CHANGE -> {
                // validated above (and no additional constraints needed for created/noChange)
            }
            default -> throw new ValidationException("Invalid actionType '" + detail.getActionType() + "' in row " + rowNumber);
        }
    }

    private void validateDetailRow(EmployeeTrainingDetailRequest detail, int rowNumber) {
        if (detail.getEmpPoid() == null) {
            throw new ValidationException("Employee is required in detail row " + rowNumber);
        }
        if (detail.getTrainingStatus() == null || detail.getTrainingStatus().isBlank()) {
            throw new ValidationException("Training status is required in detail row " + rowNumber);
        }
        if (EmployeeTrainingConstants.COMPLETED_STATUS.equalsIgnoreCase(detail.getTrainingStatus())
                && detail.getCompletedOn() == null) {
            throw new ValidationException("Completed On is required when status is COMPLETED in row " + rowNumber);
        }
    }

    private String resolveDetailActionType(String actionType, Integer detRowId) {
        if (actionType == null || actionType.isBlank()) {
            return normalizeDetRowId(detRowId) == null ? EmployeeTrainingConstants.ACTION_IS_CREATED : EmployeeTrainingConstants.ACTION_IS_UPDATED;
        }
        return actionType.trim();
    }

    private Integer normalizeDetRowId(Integer detRowId) {
        if (detRowId == null || detRowId == 0) return null;
        return detRowId;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validateHeaderDuplicateForCreate(EmployeeTrainingRequest request) {
        boolean duplicate = headerRepository.existsByCourseNameIgnoreCaseAndPeriodFromAndPeriodToAndDeletedNot(
                request.getCourseName(),
                request.getPeriodFrom(),
                request.getPeriodTo(),
                EmployeeTrainingConstants.DELETED_YES
        );
        if (duplicate) {
            throw new ValidationException("Training already exists for this course and period");
        }
    }

    private void validateHeaderDuplicateForUpdate(EmployeeTrainingHeaderEntity header, Long transactionPoid) {

        boolean duplicate = headerRepository.existsDuplicateOnUpdate(
                header.getCourseName(),
                header.getPeriodFrom(),
                header.getPeriodTo(),
                EmployeeTrainingConstants.DELETED_YES,
                transactionPoid
        );
        if (duplicate) {
            throw new ValidationException("Training already exists for this course and period");
        }
    }




    @Override
    public byte[] print(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, UserContext.getDocumentId());
        JasperReport mainReport = printService.load("HR/Employee_Induction_Rpt.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }
}
