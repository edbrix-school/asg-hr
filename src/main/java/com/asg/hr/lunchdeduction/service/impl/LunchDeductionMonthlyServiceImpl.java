package com.asg.hr.lunchdeduction.service.impl;

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
import com.asg.hr.lunchdeduction.dto.LunchDeductionActionType;
import com.asg.hr.lunchdeduction.dto.LunchDeductionDetailRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetail;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyHeader;
import com.asg.hr.lunchdeduction.repository.LunchDeductionMonthlyRepository;
import com.asg.hr.lunchdeduction.service.LunchDeductionMonthlyService;
import com.asg.hr.lunchdeduction.util.LunchDeductionMonthlyConstants;
import com.asg.hr.lunchdeduction.util.LunchDeductionMonthlyMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LunchDeductionMonthlyServiceImpl implements LunchDeductionMonthlyService {

    private static final Set<String> ALLOWED_DEDUCTION_TYPES = Set.of("FREE", "BILL_MSC", "DEDUCT");

    private final LunchDeductionMonthlyRepository repository;
    private final LunchDeductionMonthlyMapper mapper;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lists lunch deduction documents with shared pagination and filtering.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listLunchDeductions(String documentId, FilterRequestDto filterRequestDto, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequestDto);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequestDto);
        List<FilterDto> filters = documentSearchService.resolveFilters(filterRequestDto);

        RawSearchResult raw = documentSearchService.search(
                documentId,
                filters,
                operator,
                pageable,
                isDeleted,
                LunchDeductionMonthlyConstants.DOC_LABEL,
                LunchDeductionMonthlyConstants.PRIMARY_KEY
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    /**
     * Returns a single lunch deduction document.
     */
    @Override
    @Transactional(readOnly = true)
    public LunchDeductionMonthlyResponseDto getById(Long transactionPoid) {
        return mapper.toResponse(getActiveDocument(transactionPoid));
    }

    /**
     * Returns the same document payload through a getDetails contract for enrichment use cases.
     */
    @Override
    @Transactional(readOnly = true)
    public LunchDeductionMonthlyResponseDto getDetails(Long transactionPoid) {
        return getById(transactionPoid);
    }

    /**
     * Creates a lunch deduction monthly document.
     */
    @Override
    @Transactional
    public LunchDeductionMonthlyResponseDto create(LunchDeductionMonthlyRequestDto requestDto) {
        LunchDeductionMonthlyHeader header = mapper.toEntity(
                requestDto,
                UserContext.getGroupPoid(),
                UserContext.getCompanyPoid()
        );

        if (requestDto.getDetails() != null && !requestDto.getDetails().isEmpty()) {
            applyDetailChanges(header, requestDto.getDetails());
        }

        LunchDeductionMonthlyHeader saved = repository.saveAndFlush(header);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getTransactionPoid().toString());
        return mapper.toResponse(reload(saved.getTransactionPoid()));
    }

    /**
     * Updates a lunch deduction monthly document and its child rows.
     */
    @Override
    @Transactional
    public LunchDeductionMonthlyResponseDto update(Long transactionPoid, LunchDeductionMonthlyRequestDto requestDto) {
        LunchDeductionMonthlyHeader header = getActiveDocument(transactionPoid);
        LunchDeductionMonthlyHeader oldHeader = mapper.copyForLogging(header);

        mapper.updateHeader(header, requestDto);
        DetailChangeSummary detailChangeSummary = new DetailChangeSummary(0, 0, 0, 0);
        if (requestDto.getDetails() != null && !requestDto.getDetails().isEmpty()) {
            detailChangeSummary = applyDetailChanges(header, requestDto.getDetails());
        }

        repository.saveAndFlush(header);
        loggingService.logChanges(
                oldHeader,
                header,
                LunchDeductionMonthlyHeader.class,
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                LogDetailsEnum.MODIFIED,
                LunchDeductionMonthlyConstants.PRIMARY_KEY
        );

        logDetailSummary(transactionPoid, detailChangeSummary);
        return mapper.toResponse(reload(transactionPoid));
    }

    /**
     * Soft deletes a lunch deduction monthly document.
     */
    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        LunchDeductionMonthlyHeader header = getActiveDocument(transactionPoid);
        documentDeleteService.deleteDocument(
                transactionPoid,
                LunchDeductionMonthlyConstants.DOCUMENT_TABLE,
                LunchDeductionMonthlyConstants.PRIMARY_KEY,
                deleteReasonDto,
                header.getTransactionDate()
        );
    }

    /**
     * Loads fresh detail rows by executing the legacy import procedure.
     */
    @Override
    @Transactional
    public LunchDeductionMonthlyResponseDto importLunchDetails(Long transactionPoid) {
        LunchDeductionMonthlyHeader header = getActiveDocument(transactionPoid);
        if (header.getPayrollMonth() == null) {
            throw new ValidationException("Payroll month is required to import lunch details");
        }

        try {
            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName(LunchDeductionMonthlyConstants.IMPORT_PROCEDURE)
                    .declareParameters(
                            new SqlParameter("P_TRANSACTION_NO", Types.NUMERIC),
                            new SqlParameter("P_LOGIN_USER_POID", Types.NUMERIC),
                            new SqlParameter("P_PAYROLL_MONTH", Types.DATE),
                            new SqlParameter("P_OTHER_PARAMETERS", Types.VARCHAR),
                            new SqlOutParameter("P_STATUS", Types.VARCHAR)
                    );

            Map<String, Object> output = call.execute(Map.of(
                    "P_TRANSACTION_NO", transactionPoid,
                    "P_LOGIN_USER_POID", UserContext.getUserPoid(),
                    "P_PAYROLL_MONTH", java.sql.Date.valueOf(header.getPayrollMonth()),
                    "P_OTHER_PARAMETERS", ""
            ));

            String status = Optional.ofNullable((String) output.get("P_STATUS")).orElse("ERROR : Procedure returned no status");
            if (status.toUpperCase().contains("ERROR")) {
                throw new ValidationException(status);
            }

            entityManager.clear();
            loggingService.createLogSummaryEntry(
                    UserContext.getDocumentId(),
                    transactionPoid.toString(),
                    "Lunch Records loaded.."
            );
            return mapper.toResponse(reload(transactionPoid));
        } catch (DataAccessException exception) {
            throw new ValidationException("Error while loading lunch deduction details: " + exception.getMostSpecificCause().getMessage());
        }
    }

    private LunchDeductionMonthlyHeader getActiveDocument(Long transactionPoid) {
        return repository.findDetailedByTransactionPoidAndDeletedNot(transactionPoid, "Y")
                .orElseThrow(() -> new ResourceNotFoundException(
                        LunchDeductionMonthlyConstants.MODULE_NAME,
                        LunchDeductionMonthlyConstants.MODULE_KEY,
                        transactionPoid
                ));
    }

    private LunchDeductionMonthlyHeader reload(Long transactionPoid) {
        entityManager.clear();
        return getActiveDocument(transactionPoid);
    }

    private DetailChangeSummary applyDetailChanges(
            LunchDeductionMonthlyHeader header,
            List<LunchDeductionDetailRequestDto> detailRequests
    ) {
        Map<Long, LunchDeductionMonthlyDetail> existingByRowId = new HashMap<>();
        for (LunchDeductionMonthlyDetail detail : header.getDetails()) {
            existingByRowId.put(detail.getId().getDetRowId(), detail);
        }

        long nextDetRowId = existingByRowId.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1L;
        long created = 0L;
        long updated = 0L;
        long deleted = 0L;
        long unchanged = 0L;
        List<LunchDeductionMonthlyDetail> toRemove = new ArrayList<>();

        for (LunchDeductionDetailRequestDto detailRequest : detailRequests) {
            validateDetailRequest(detailRequest);

            if (detailRequest.getActionType() == LunchDeductionActionType.CREATE) {
                LunchDeductionMonthlyDetail detail = mapper.toDetailEntity(detailRequest, nextDetRowId++);
                header.addDetail(detail);
                created++;
                continue;
            }

            if (detailRequest.getDetRowId() == null) {
                throw new ValidationException("detRowId is required for UPDATE and DELETE detail actions");
            }

            LunchDeductionMonthlyDetail existingDetail = existingByRowId.get(detailRequest.getDetRowId());
            if (existingDetail == null) {
                throw new ResourceNotFoundException("Lunch deduction detail", "detRowId", detailRequest.getDetRowId());
            }

            if (detailRequest.getActionType() == LunchDeductionActionType.DELETE) {
                toRemove.add(existingDetail);
                deleted++;
                continue;
            }

            if (hasMeaningfulUpdate(existingDetail, detailRequest)) {
                mapper.updateDetail(existingDetail, detailRequest);
                updated++;
            } else {
                unchanged++;
            }
        }

        toRemove.forEach(header::removeDetail);
        return new DetailChangeSummary(created, updated, deleted, unchanged);
    }

    private boolean hasMeaningfulUpdate(LunchDeductionMonthlyDetail existingDetail, LunchDeductionDetailRequestDto detailRequest) {
        return !java.util.Objects.equals(existingDetail.getEmployeePoid(), detailRequest.getEmployeePoid())
                || !java.util.Objects.equals(existingDetail.getDeductionType(), detailRequest.getDeductionType())
                || !java.util.Objects.equals(existingDetail.getMonthDays(), detailRequest.getMonthDays())
                || !java.util.Objects.equals(existingDetail.getOffDays(), detailRequest.getOffDays())
                || !java.util.Objects.equals(existingDetail.getLunchDays(), detailRequest.getLunchDays())
                || !java.util.Objects.equals(existingDetail.getCostPerDay(), detailRequest.getCostPerDay())
                || !java.util.Objects.equals(existingDetail.getUserId(), detailRequest.getUserId())
                || !java.util.Objects.equals(existingDetail.getUserName(), detailRequest.getUserName())
                || !java.util.Objects.equals(existingDetail.getRemarks(), detailRequest.getRemarks());
    }

    private void validateDetailRequest(LunchDeductionDetailRequestDto detailRequest) {
        if (detailRequest.getDeductionType() != null && !ALLOWED_DEDUCTION_TYPES.contains(detailRequest.getDeductionType())) {
            throw new ValidationException("Deduction type must be one of FREE, BILL_MSC, or DEDUCT");
        }

        if (detailRequest.getMonthDays() != null && detailRequest.getOffDays() != null
                && detailRequest.getOffDays() > detailRequest.getMonthDays()) {
            throw new ValidationException("Off days cannot exceed month days");
        }
    }

    private void logDetailSummary(Long transactionPoid, DetailChangeSummary detailChangeSummary) {
        if (detailChangeSummary.total() == 0L) {
            return;
        }

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                transactionPoid.toString(),
                "Detail changes : created=" + detailChangeSummary.created()
                        + ", updated=" + detailChangeSummary.updated()
                        + ", deleted=" + detailChangeSummary.deleted()
                        + ", noChanges=" + detailChangeSummary.unchanged()
        );
    }

    private record DetailChangeSummary(long created, long updated, long deleted, long unchanged) {
        private long total() {
            return created + updated + deleted + unchanged;
        }
    }
}