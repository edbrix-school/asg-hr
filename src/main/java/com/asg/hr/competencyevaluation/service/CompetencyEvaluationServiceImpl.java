package com.asg.hr.competencyevaluation.service;

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
import com.asg.common.lib.utility.ASGHelperUtils;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.competency.entity.CompetencyMasterEntity;
import com.asg.hr.competency.entity.HrCompetencySchedule;
import com.asg.hr.competency.repository.CompetencyMasterRepository;
import com.asg.hr.competency.repository.HrCompetencyScheduleRepository;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationCalculateScoresRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationCalculateScoresResponseDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationRequestDto;
import com.asg.hr.competencyevaluation.dto.CompetencyEvaluationResponseDto;
import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationDtl;
import com.asg.hr.competencyevaluation.entity.HrCompetencyEvaluationHdr;
import com.asg.hr.competencyevaluation.repository.HrCompetencyEvaluationDtlRepository;
import com.asg.hr.competencyevaluation.repository.HrCompetencyEvaluationHdrRepository;
import com.asg.hr.competencyevaluation.util.CompetencyEvaluationConstants;
import com.asg.hr.competencyevaluation.util.CompetencyEvaluationScores;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetencyEvaluationServiceImpl implements CompetencyEvaluationService {

    private static final String TABLE_NAME = "HR_COMPETENCY_EVALUATION_HDR";
    private static final String PRIMARY_KEY = "TRANSACTION_POID";
    private static final String DESCRIPTION_FIELD = "DOC_REF";
    private static final String ENTITY_LABEL = "Competency evaluation";
    private static final String POID_FIELD = "transactionPoid";
    private static final String COMPLETED = "COMPLETED";
    private static final String DELETED_NO = "N";
    private static final String ACTIVE_YES = "Y";

    private final HrCompetencyEvaluationHdrRepository hdrRepository;
    private final HrCompetencyEvaluationDtlRepository dtlRepository;
    private final HrCompetencyScheduleRepository scheduleRepository;
    private final CompetencyMasterRepository competencyMasterRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final LovDataService lovDataService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public CompetencyEvaluationResponseDto create(CompetencyEvaluationRequestDto request) {
        HrCompetencySchedule schedule = loadScheduleOrThrow(request.getCompSchedulePoid());
        validateScheduleOverlapsCurrentYear(schedule);
        validateEvaluationDateIfPresent(schedule, request.getEvaluationDate());
        validateCompetencyLines(request, false);
        validateDetailRatings(request, false);

        HrCompetencyEvaluationHdr hdr = buildHeaderFromRequest(request);
        hdr.setDeleted(DELETED_NO);
        hdr.setCreatedBy(ASGHelperUtils.getCurrentUser());
        hdr.setCreatedDate(LocalDateTime.now());
        hdr = hdrRepository.save(hdr);

        persistDetails(hdr, request.getDetails());
        applyScoresToHeader(hdr.getTransactionPoid());

        hdr = hdrRepository.findActiveById(hdr.getTransactionPoid()).orElse(hdr);
        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                hdr.getTransactionPoid().toString(),
                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), hdr.getDocRef()));
        return mapToResponse(hdr, dtlRepository.findByTransactionPoidOrderByDetRowId(hdr.getTransactionPoid()));
    }

    @Override
    @Transactional
    public CompetencyEvaluationResponseDto update(Long transactionPoid, CompetencyEvaluationRequestDto request) {
        HrCompetencyEvaluationHdr hdr = hdrRepository.findActiveById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_LABEL, POID_FIELD, transactionPoid));
        rejectIfCompleted(hdr);

        if (request.getDetails() != null) {
            for (int i = 0; i < request.getDetails().size(); i++) {
                validateDetailAction(request.getDetails().get(i), i + 1);
            }
        }
        HrCompetencySchedule schedule = loadScheduleOrThrow(request.getCompSchedulePoid());
        validateScheduleOverlapsCurrentYear(schedule);
        validateEvaluationDateIfPresent(schedule, request.getEvaluationDate());
        validateCompetencyLines(request, true);
        validateDetailRatings(request, true);

        HrCompetencyEvaluationHdr oldCopy = snapshot(hdr);

        hdr.setDocRef(request.getDocRef());
        hdr.setEmployeePoid(request.getEmployeePoid());
        hdr.setDepartmentPoid(request.getDepartmentPoid());
        hdr.setDesignationPoid(request.getDesignationPoid());
        hdr.setReviewedByPoid(request.getReviewedByPoid());
        hdr.setCompSchedulePoid(request.getCompSchedulePoid());
        if (request.getTransactionDate() != null) {
            hdr.setTransactionDate(request.getTransactionDate());
        }
        hdr.setEvaluationDate(request.getEvaluationDate());
        hdr.setStatus(request.getStatus());
        hdr.setHodRemarks(request.getHodRemarks());
        hdr.setEmployeeRemarks(request.getEmployeeRemarks());
        hdr.setReviewerComments(request.getReviewerComments());
        hdr.setTrainingNeeds(request.getTrainingNeeds());
        hdr.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());

        hdrRepository.save(hdr);
        reconcileDetailsOnUpdate(transactionPoid, hdr, request.getDetails());
        applyScoresToHeader(transactionPoid);

        hdr = hdrRepository.findActiveById(transactionPoid).orElse(hdr);
        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                String.format("%s %s", LogDetailsEnum.MODIFIED.getDescription(), hdr.getDocRef()));
        loggingService.logChanges(oldCopy, hdr, HrCompetencyEvaluationHdr.class,
                UserContext.getDocumentId(), transactionPoid.toString(), LogDetailsEnum.MODIFIED, PRIMARY_KEY);

        return mapToResponse(hdr, dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid));
    }

    @Override
    @Transactional(readOnly = true)
    public CompetencyEvaluationResponseDto getById(Long transactionPoid) {
        HrCompetencyEvaluationHdr hdr = hdrRepository.findActiveById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_LABEL, POID_FIELD, transactionPoid));
        List<HrCompetencyEvaluationDtl> lines = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        return mapToResponse(hdr, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(FilterRequestDto filterRequest, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filters = documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentSearchService.search(
                UserContext.getDocumentId(),
                filters,
                operator,
                pageable,
                isDeleted,
                DESCRIPTION_FIELD,
                PRIMARY_KEY
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        hdrRepository.findActiveById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_LABEL, POID_FIELD, transactionPoid));
        documentDeleteService.deleteDocument(transactionPoid, TABLE_NAME, PRIMARY_KEY, deleteReasonDto, null);
        log.info("Soft deleted competency evaluation {}", transactionPoid);
    }

    @Override
    @Transactional
    public CompetencyEvaluationResponseDto calculateScores(Long transactionPoid) {
        HrCompetencyEvaluationHdr hdr = hdrRepository.findActiveById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_LABEL, POID_FIELD, transactionPoid));
        rejectIfCompleted(hdr);

        List<HrCompetencyEvaluationDtl> lines = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        validateAllRatingsOnLines(lines);
        CompetencyEvaluationScores.ScoreResult scores = CompetencyEvaluationScores.calculate(
                lines.stream().map(HrCompetencyEvaluationDtl::getRating).toList(),
                lines.stream().map(HrCompetencyEvaluationDtl::getEmployeeAgreed).toList()
        );
        hdr.setTotalRating(scores.totalRating());
        hdr.setAvgRatingPercent(scores.avgRatingPercent());
        hdr.setEmployeeAgreedPercent(scores.employeeAgreedPercent());
        hdr.setLastModifiedBy(ASGHelperUtils.getCurrentUser());
        hdr.setLastModifiedDate(LocalDateTime.now());
        hdrRepository.save(hdr);

        return mapToResponse(hdr, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public CompetencyEvaluationCalculateScoresResponseDto calculateScoresFromDetails(
            CompetencyEvaluationCalculateScoresRequestDto request) {
        List<String> ratings = request.getDetails().stream()
                .map(CompetencyEvaluationCalculateScoresRequestDto.DetailRatingDto::getRating)
                .toList();
        List<String> employeeAgreed = request.getDetails().stream()
                .map(CompetencyEvaluationCalculateScoresRequestDto.DetailRatingDto::getEmployeeAgreed)
                .toList();
        CompetencyEvaluationScores.ScoreResult scores = CompetencyEvaluationScores.calculate(ratings, employeeAgreed);
        return CompetencyEvaluationCalculateScoresResponseDto.builder()
                .totalRating(scores.totalRating())
                .avgRatingPercent(scores.avgRatingPercent())
                .employeeAgreedPercent(scores.employeeAgreedPercent())
                .details(request.getDetails())
                .build();
    }

    private void rejectIfCompleted(HrCompetencyEvaluationHdr hdr) {
        if (hdr.getStatus() != null && COMPLETED.equalsIgnoreCase(hdr.getStatus().trim())) {
            throw new ValidationException("Completed reviews cannot be modified unless reopened by HR.");
        }
    }

    private HrCompetencyEvaluationHdr snapshot(HrCompetencyEvaluationHdr hdr) {
        HrCompetencyEvaluationHdr copy = new HrCompetencyEvaluationHdr();
        BeanUtils.copyProperties(hdr, copy);
        return copy;
    }

    private HrCompetencyEvaluationHdr buildHeaderFromRequest(CompetencyEvaluationRequestDto request) {
        return HrCompetencyEvaluationHdr.builder()
                .groupPoid(UserContext.getGroupPoid())
                .companyPoid(UserContext.getCompanyPoid())
                .docRef(request.getDocRef())
                .employeePoid(request.getEmployeePoid())
                .departmentPoid(request.getDepartmentPoid())
                .designationPoid(request.getDesignationPoid())
                .reviewedByPoid(request.getReviewedByPoid())
                .compSchedulePoid(request.getCompSchedulePoid())
                .transactionDate(request.getTransactionDate())
                .evaluationDate(request.getEvaluationDate())
                .status(request.getStatus())
                .hodRemarks(request.getHodRemarks())
                .employeeRemarks(request.getEmployeeRemarks())
                .reviewerComments(request.getReviewerComments())
                .trainingNeeds(request.getTrainingNeeds())
                .build();
    }

    private void persistDetails(HrCompetencyEvaluationHdr hdr, List<CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto> detailDtos) {
        List<Long> rowIds = assignRowIds(detailDtos);
        String user = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < detailDtos.size(); i++) {
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto dto = detailDtos.get(i);
            HrCompetencyEvaluationDtl line = HrCompetencyEvaluationDtl.builder()
                    .transactionPoid(hdr.getTransactionPoid())
                    .detRowId(rowIds.get(i))
                    .competencyPoid(dto.getCompetencyPoid())
                    .compSchedulePoid(hdr.getCompSchedulePoid())
                    .rating(dto.getRating())
                    .remarks(dto.getHodComments())
                    .employeeAgreed(dto.getEmployeeAgreed())
                    .employeeComments(dto.getEmployeeComments())
                    .build();
            line.setCreatedBy(user);
            line.setCreatedDate(now);
            dtlRepository.save(line);
        }
    }

    private List<Long> assignRowIds(List<CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto> detailDtos) {
        List<Long> ids = new ArrayList<>(detailDtos.size());
        for (int i = 0; i < detailDtos.size(); i++) {
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto dto = detailDtos.get(i);
            if (dto.getDetRowId() != null) {
                ids.add(dto.getDetRowId());
            } else {
                ids.add((long) (i + 1));
            }
        }
        long distinct = ids.stream().distinct().count();
        if (distinct != ids.size()) {
            throw new ValidationException("Duplicate detail row ids are not allowed");
        }
        return ids;
    }

    private void applyScoresToHeader(Long transactionPoid) {
        List<HrCompetencyEvaluationDtl> lines = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        HrCompetencyEvaluationHdr hdr = hdrRepository.findActiveById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_LABEL, POID_FIELD, transactionPoid));
        CompetencyEvaluationScores.ScoreResult scores = CompetencyEvaluationScores.calculate(
                lines.stream().map(HrCompetencyEvaluationDtl::getRating).toList(),
                lines.stream().map(HrCompetencyEvaluationDtl::getEmployeeAgreed).toList()
        );
        hdr.setTotalRating(scores.totalRating());
        hdr.setAvgRatingPercent(scores.avgRatingPercent());
        hdr.setEmployeeAgreedPercent(scores.employeeAgreedPercent());
        hdrRepository.save(hdr);
    }

    private HrCompetencySchedule loadScheduleOrThrow(Long schedulePoid) {
        if (schedulePoid == null) {
            throw new ValidationException("Schedule is required.");
        }
        Long loginGroupPoid = UserContext.getGroupPoid();
        if (loginGroupPoid == null) {
            throw new ValidationException(
                    "Your login session has no group (groupPoid). Competency schedules are scoped by group; sign in again or contact support.");
        }
        HrCompetencySchedule schedule = scheduleRepository.findById(schedulePoid)
                .orElseThrow(() -> new ValidationException(
                        "No competency review schedule found for id " + schedulePoid + "."));
        if (isScheduleMarkedDeleted(schedule)) {
            throw new ValidationException(
                    "Competency review schedule " + schedulePoid + " is deleted. Restore it in HR Competency Schedule or choose another schedule.");
        }
        if (!Objects.equals(schedule.getGroupPoid(), loginGroupPoid)) {
            throw new ValidationException(
                    "Competency review schedule " + schedulePoid + " belongs to group " + schedule.getGroupPoid()
                            + ", but your session is for group " + loginGroupPoid
                            + ". Use a schedule for your group or switch login group.");
        }
        return schedule;
    }

    
    private static boolean isScheduleMarkedDeleted(HrCompetencySchedule schedule) {
        String deleted = schedule.getDeleted();
        if (deleted == null || deleted.isBlank()) {
            return false;
        }
        return "Y".equalsIgnoreCase(deleted.trim());
    }

    private void validateScheduleOverlapsCurrentYear(HrCompetencySchedule schedule) {
        LocalDate yearStart = Year.now().atDay(1);
        LocalDate yearEnd = Year.now().atMonth(Month.DECEMBER).atEndOfMonth();
        if (schedule.getPeriodFrom() == null || schedule.getPeriodTo() == null) {
            throw new ValidationException("Schedule period is not defined.");
        }
        boolean overlaps = !schedule.getPeriodTo().isBefore(yearStart) && !schedule.getPeriodFrom().isAfter(yearEnd);
        if (!overlaps) {
            throw new ValidationException("Review schedule must exist for the current year before forms can be generated.");
        }
    }

    private void validateEvaluationDateIfPresent(HrCompetencySchedule schedule, LocalDate evaluationDate) {
        if (evaluationDate == null) {
            return;
        }
        if (evaluationDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Evaluation date must be today or a future date.");
        }
        if (evaluationDate.isBefore(schedule.getPeriodFrom()) || evaluationDate.isAfter(schedule.getPeriodTo())) {
            throw new ValidationException("Evaluation date must be within the review period.");
        }
    }

    private void validateDetailAction(CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto detail,
                                      int rowNumber) {
        if (detail == null) {
            return;
        }
        String action = resolveDetailActionType(detail.getActionType(), detail.getDetRowId());
        if ((CompetencyEvaluationConstants.ACTION_IS_UPDATED.equals(action)
                || CompetencyEvaluationConstants.ACTION_IS_DELETED.equals(action)
                || CompetencyEvaluationConstants.ACTION_NO_CHANGE.equals(action))
                && detail.getDetRowId() == null) {
            throw new ValidationException("detRowId is required for " + action + " in row " + rowNumber);
        }
        switch (action) {
            case CompetencyEvaluationConstants.ACTION_IS_UPDATED,
                    CompetencyEvaluationConstants.ACTION_IS_DELETED,
                    CompetencyEvaluationConstants.ACTION_IS_CREATED,
                    CompetencyEvaluationConstants.ACTION_NO_CHANGE -> {
                // validated above
            }
            default -> throw new ValidationException(
                    "Invalid actionType '" + detail.getActionType() + "' in row " + rowNumber);
        }
    }

    private String resolveDetailActionType(String actionType, Long detRowId) {
        if (actionType == null || actionType.isBlank()) {
            return detRowId == null
                    ? CompetencyEvaluationConstants.ACTION_IS_CREATED
                    : CompetencyEvaluationConstants.ACTION_IS_UPDATED;
        }
        return actionType.trim();
    }

    private void reconcileDetailsOnUpdate(Long transactionPoid, HrCompetencyEvaluationHdr hdr,
                                          List<CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto> detailDtos) {
        if (detailDtos == null || detailDtos.isEmpty()) {
            return;
        }
        List<HrCompetencyEvaluationDtl> existing = dtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, HrCompetencyEvaluationDtl> byDetRowId = new HashMap<>();
        long maxDetRowId = 0L;
        for (HrCompetencyEvaluationDtl e : existing) {
            byDetRowId.put(e.getDetRowId(), e);
            maxDetRowId = Math.max(maxDetRowId, e.getDetRowId());
        }

        String user = ASGHelperUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < detailDtos.size(); i++) {
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto dto = detailDtos.get(i);
            if (dto == null) {
                continue;
            }
            int rowNum = i + 1;
            String action = resolveDetailActionType(dto.getActionType(), dto.getDetRowId());

            switch (action) {
                case CompetencyEvaluationConstants.ACTION_IS_CREATED -> {
                    maxDetRowId++;
                    HrCompetencyEvaluationDtl line = buildDetailEntity(hdr, dto, maxDetRowId);
                    line.setCreatedBy(user);
                    line.setCreatedDate(now);
                    dtlRepository.save(line);
                    byDetRowId.put(maxDetRowId, line);
                }
                case CompetencyEvaluationConstants.ACTION_IS_UPDATED -> {
                    HrCompetencyEvaluationDtl line = byDetRowId.get(dto.getDetRowId());
                    if (line == null) {
                        throw new ValidationException("Detail row not found for detRowId=" + dto.getDetRowId() + " (row " + rowNum + ")");
                    }
                    applyDetailFromDto(line, hdr, dto);
                    line.setLastModifiedBy(user);
                    line.setLastModifiedDate(now);
                    dtlRepository.save(line);
                }
                case CompetencyEvaluationConstants.ACTION_IS_DELETED -> {
                    HrCompetencyEvaluationDtl line = byDetRowId.get(dto.getDetRowId());
                    if (line == null) {
                        throw new ValidationException("Detail row not found for detRowId=" + dto.getDetRowId() + " (row " + rowNum + ")");
                    }
                    dtlRepository.delete(line);
                    byDetRowId.remove(dto.getDetRowId());
                }
                case CompetencyEvaluationConstants.ACTION_NO_CHANGE -> {
                    if (!byDetRowId.containsKey(dto.getDetRowId())) {
                        throw new ValidationException("Detail row not found for detRowId=" + dto.getDetRowId() + " (row " + rowNum + ")");
                    }
                }
                default -> throw new ValidationException("Unsupported actionType in row " + rowNum);
            }
        }
    }

    private static HrCompetencyEvaluationDtl buildDetailEntity(HrCompetencyEvaluationHdr hdr,
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto dto, long detRowId) {
        return HrCompetencyEvaluationDtl.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .detRowId(detRowId)
                .competencyPoid(dto.getCompetencyPoid())
                .compSchedulePoid(hdr.getCompSchedulePoid())
                .rating(dto.getRating())
                .remarks(dto.getHodComments())
                .employeeAgreed(dto.getEmployeeAgreed())
                .employeeComments(dto.getEmployeeComments())
                .build();
    }

    private static void applyDetailFromDto(HrCompetencyEvaluationDtl line, HrCompetencyEvaluationHdr hdr,
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto dto) {
        line.setCompetencyPoid(dto.getCompetencyPoid());
        line.setCompSchedulePoid(hdr.getCompSchedulePoid());
        line.setRating(dto.getRating());
        line.setRemarks(dto.getHodComments());
        line.setEmployeeAgreed(dto.getEmployeeAgreed());
        line.setEmployeeComments(dto.getEmployeeComments());
    }

    private void validateCompetencyLines(CompetencyEvaluationRequestDto request, boolean isUpdate) {
        Long groupPoid = UserContext.getGroupPoid();
        List<CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto> details = request.getDetails();
        if (details == null || details.isEmpty()) {
            throw new ValidationException("At least one detail line is required");
        }
        for (int i = 0; i < details.size(); i++) {
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto line = details.get(i);
            if (isUpdate) {
                String action = resolveDetailActionType(line.getActionType(), line.getDetRowId());
                if (CompetencyEvaluationConstants.ACTION_IS_DELETED.equals(action)
                        || CompetencyEvaluationConstants.ACTION_NO_CHANGE.equals(action)) {
                    continue;
                }
            }
            CompetencyMasterEntity comp = competencyMasterRepository
                    .findByIdAndGroupPoidAndNotDeleted(line.getCompetencyPoid(), groupPoid)
                    .orElseThrow(() -> new ValidationException("Competency is not available: " + line.getCompetencyPoid()));
            if (!isActive(comp.getActive())) {
                throw new ValidationException("Competency must be active in Employee Review Master: " + line.getCompetencyPoid());
            }
        }
    }

    private void validateDetailRatings(CompetencyEvaluationRequestDto request, boolean isUpdate) {
        String message = isUpdate
                ? "Every line with isCreated or isUpdated must have a rating before save"
                : "Every competency must have a rating before save";
        List<CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto> details = request.getDetails();
        for (int i = 0; i < details.size(); i++) {
            CompetencyEvaluationRequestDto.CompetencyEvaluationDetailRequestDto line = details.get(i);
            if (isUpdate) {
                String action = resolveDetailActionType(line.getActionType(), line.getDetRowId());
                if (CompetencyEvaluationConstants.ACTION_IS_DELETED.equals(action)
                        || CompetencyEvaluationConstants.ACTION_NO_CHANGE.equals(action)) {
                    continue;
                }
            }
            if (line.getRating() == null || line.getRating().isBlank()) {
                throw new ValidationException(message + " (row " + (i + 1) + ")");
            }
        }
    }

    private static boolean isActive(String active) {
        return active != null && ACTIVE_YES.equalsIgnoreCase(active.trim());
    }

    private void validateAllRatingsOnLines(List<HrCompetencyEvaluationDtl> lines) {
        boolean anyMissing = lines.stream()
                .map(HrCompetencyEvaluationDtl::getRating)
                .anyMatch(r -> r == null || r.isBlank());
        if (anyMissing) {
            throw new ValidationException("Every competency must have a rating before calculating scores.");
        }
    }

    private CompetencyEvaluationResponseDto mapToResponse(HrCompetencyEvaluationHdr hdr, List<HrCompetencyEvaluationDtl> lines) {
        List<CompetencyEvaluationResponseDto.CompetencyEvaluationDetailResponseDto> detailDtos = lines.stream()
                .map(line -> CompetencyEvaluationResponseDto.CompetencyEvaluationDetailResponseDto.builder()
                        .detRowId(line.getDetRowId())
                        .competencyPoid(line.getCompetencyPoid())
                        .compSchedulePoid(line.getCompSchedulePoid())
                        .rating(line.getRating())
                        .hodComments(line.getRemarks())
                        .employeeAgreed(line.getEmployeeAgreed())
                        .employeeComments(line.getEmployeeComments())
                        .actionType(CompetencyEvaluationConstants.ACTION_NO_CHANGE)
                        .build())
                .toList();

        CompetencyEvaluationResponseDto response = CompetencyEvaluationResponseDto.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .docRef(hdr.getDocRef())
                .transactionDate(hdr.getTransactionDate())
                .employeePoid(hdr.getEmployeePoid())
                .departmentPoid(hdr.getDepartmentPoid())
                .designationPoid(hdr.getDesignationPoid())
                .reviewedByPoid(hdr.getReviewedByPoid())
                .compSchedulePoid(hdr.getCompSchedulePoid())
                .evaluationDate(hdr.getEvaluationDate())
                .status(hdr.getStatus())
                .hodRemarks(hdr.getHodRemarks())
                .employeeRemarks(hdr.getEmployeeRemarks())
                .reviewerComments(hdr.getReviewerComments())
                .trainingNeeds(hdr.getTrainingNeeds())
                .totalRating(hdr.getTotalRating())
                .avgRatingPercent(hdr.getAvgRatingPercent())
                .employeeAgreedPercent(hdr.getEmployeeAgreedPercent())
                .createdBy(hdr.getCreatedBy())
                .createdDate(hdr.getCreatedDate())
                .lastModifiedBy(hdr.getLastModifiedBy())
                .lastModifiedDate(hdr.getLastModifiedDate())
                .details(detailDtos)
                .build();
        enrichLovDetails(response);
        return response;
    }

    private void enrichLovDetails(CompetencyEvaluationResponseDto response) {
        if (response == null) {
            return;
        }

        response.setEmployeeDet(resolveLovByPoid(response.getEmployeePoid(), CompetencyEvaluationConstants.EMPLOYEE_LOV));
        response.setReviewedByDet(resolveLovByPoid(response.getReviewedByPoid(), CompetencyEvaluationConstants.EMPLOYEE_LOV));
        response.setCompScheduleDet(resolveLovByPoid(response.getCompSchedulePoid(), CompetencyEvaluationConstants.COMP_SCHEDULE_LOV));
        response.setDepartmentDet(getLovOrEmpty(
                fetchDepartmentDetails(toDistinctPositiveList(response.getDepartmentPoid())),
                response.getDepartmentPoid()));
        response.setDesignationDet(getLovOrEmpty(
                fetchDesignationDetails(toDistinctPositiveList(response.getDesignationPoid())),
                response.getDesignationPoid()));

        if (response.getDetails() == null || response.getDetails().isEmpty()) {
            return;
        }

        List<Long> competencyPoids = response.getDetails().stream()
                .map(CompetencyEvaluationResponseDto.CompetencyEvaluationDetailResponseDto::getCompetencyPoid)
                .filter(Objects::nonNull)
                .filter(p -> p > 0)
                .distinct()
                .toList();
        Map<Long, LovGetListDto> competencyMap = fetchCompetencyDetails(competencyPoids);
        LovGetListDto headerScheduleDet = response.getCompScheduleDet();

        for (CompetencyEvaluationResponseDto.CompetencyEvaluationDetailResponseDto detail : response.getDetails()) {
            detail.setCompetencyDet(getLovOrEmpty(competencyMap, detail.getCompetencyPoid()));
            if (detail.getCompSchedulePoid() != null
                    && detail.getCompSchedulePoid().equals(response.getCompSchedulePoid())
                    && headerScheduleDet != null
                    && headerScheduleDet.getPoid() != null) {
                detail.setCompScheduleDet(headerScheduleDet);
            } else {
                detail.setCompScheduleDet(resolveLovByPoid(
                        detail.getCompSchedulePoid(), CompetencyEvaluationConstants.COMP_SCHEDULE_LOV));
            }
        }
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

    private LovGetListDto getLovOrEmpty(Map<Long, LovGetListDto> lovMap, Long poid) {
        if (poid == null || lovMap == null || lovMap.isEmpty()) {
            return new LovGetListDto();
        }
        return lovMap.getOrDefault(poid, new LovGetListDto());
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
        query.setParameter(CompetencyEvaluationConstants.PARAM_POIDS, departmentPoids);
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
        query.setParameter(CompetencyEvaluationConstants.PARAM_POIDS, designationPoids);
        return toLovMap(query);
    }

    private Map<Long, LovGetListDto> fetchCompetencyDetails(List<Long> competencyPoids) {
        if (competencyPoids.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT COMPETENCY_POID AS POID,
                       COMPETENCY_CODE AS CODE,
                       COMPETENCY_DESCRIPTION AS DESCRIPTION
                  FROM HR_COMPETENCY_MASTER
                 WHERE COMPETENCY_POID IN (:poids)
                 ORDER BY SEQNO
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(CompetencyEvaluationConstants.PARAM_POIDS, competencyPoids);
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

    private List<Long> toDistinctPositiveList(Long... poids) {
        Set<Long> resolved = new LinkedHashSet<>();
        for (Long poid : poids) {
            if (poid != null && poid > 0) {
                resolved.add(poid);
            }
        }
        return new ArrayList<>(resolved);
    }
}
