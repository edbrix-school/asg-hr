package com.asg.hr.lunchdeductionmonthly.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.hr.exceptions.ResourceAlreadyExistsException;
import com.asg.hr.exceptions.ResourceNotFoundException;
import com.asg.hr.exceptions.ValidationException;
import com.asg.hr.lunchdeductionmonthly.dto.*;
import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchDtl;
import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchHdr;
import com.asg.hr.lunchdeductionmonthly.mapper.HrLunchDeductionMapper;
import com.asg.hr.lunchdeductionmonthly.repository.HrLunchDeductionProcRepository;
import com.asg.hr.lunchdeductionmonthly.repository.HrMonthlyLunchDtlRepository;
import com.asg.hr.lunchdeductionmonthly.repository.HrMonthlyLunchHdrRepository;
import com.asg.hr.lunchdeductionmonthly.service.HrLunchDeductionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrLunchDeductionServiceImpl implements HrLunchDeductionService {

    private final HrMonthlyLunchHdrRepository hdrRepository;
    private final HrMonthlyLunchDtlRepository dtlRepository;
    private final HrLunchDeductionProcRepository procRepository;
    private final HrLunchDeductionMapper mapper;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public HrLunchDeductionResponse create(HrLunchDeductionRequest request) {
        Long companyPoid = UserContext.getCompanyPoid();

        boolean exists = hdrRepository.existsByPayrollMonthAndDeletedAndCompanyPoid(
                request.getPayrollMonth(), "N", companyPoid);
        if (exists) {
            throw new ResourceAlreadyExistsException("Lunch Deduction", "payroll month");
        }

        HrMonthlyLunchHdr hdr = mapper.toEntity(request);
        hdr.setCompanyPoid(companyPoid);
        hdr.setTransactionDate(java.time.LocalDate.now());
        HrMonthlyLunchHdr saved = hdrRepository.saveAndFlush(hdr);
        entityManager.refresh(saved);

        List<HrLunchDeductionDtlRequest> details = request.getDetails() == null ? List.of() : request.getDetails();
        List<HrMonthlyLunchDtl> detailEntities = mapper.toDtlEntityList(saved.getTransactionPoid(), details);
        if (!detailEntities.isEmpty()) {
            dtlRepository.saveAll(detailEntities);
            String docId = UserContext.getDocumentId();
            String docKeyPoid = saved.getTransactionPoid().toString();
            for (HrMonthlyLunchDtl entity : detailEntities) {
                String logDetail = String.format("Row Created on Lunch Detail with detRowId: %s", entity.getDetRowId());
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            }
        }

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                saved.getTransactionPoid().toString(),
                LogDetailsEnum.CREATED.getDescription() + " " + saved.getDocRef()
        );
        
        HrLunchDeductionResponse response = mapper.toResponse(saved);
        response.setDetails(mapper.toDtlResponseList(detailEntities));
        return response;
    }

    @Override
    @Transactional
    public HrLunchDeductionResponse update(Long transactionPoid, HrLunchDeductionRequest request) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);

        mapper.updateEntity(hdr, request);
        HrMonthlyLunchHdr saved = hdrRepository.saveAndFlush(hdr);

        updateLunchDeductionDetails(transactionPoid, request.getDetails());

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), transactionPoid.toString());
        
        List<HrMonthlyLunchDtl> updatedDetails = dtlRepository.findByTransactionPoid(transactionPoid);
        HrLunchDeductionResponse response = mapper.toResponse(saved);
        response.setDetails(mapper.toDtlResponseList(updatedDetails));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public HrLunchDeductionResponse getById(Long transactionPoid) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);
        List<HrMonthlyLunchDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        HrLunchDeductionResponse response = mapper.toResponse(hdr);
        response.setDetails(mapper.toDtlResponseList(details));
        return response;
    }

    @Override
    @Transactional
    public HrLunchDeductionLoadDto loadAndProcess(Long transactionPoid) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);

        procRepository.loadLunchDetails(transactionPoid, UserContext.getUserPoid(), hdr.getPayrollMonth());

        List<HrMonthlyLunchDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        return HrLunchDeductionLoadDto.builder()
                .lunchDetails(mapper.toDtlResponseList(details))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(FilterRequestDto filterRequest, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filters = documentSearchService.resolveFilters(filterRequest);

        RawSearchResult raw = documentSearchService.search(
                UserContext.getDocumentId(), filters, operator, pageable, isDeleted, "DESCRIPTION", "TRANSACTION_POID");
        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);

        documentDeleteService.deleteDocument(
                transactionPoid, "HR_MONTHLY_LUNCH_HDR", "TRANSACTION_POID", deleteReasonDto, null);

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), transactionPoid.toString());
    }

    private void updateLunchDeductionDetails(Long transactionPoid, List<HrLunchDeductionDtlRequest> detailRequests) {
        List<HrLunchDeductionDtlRequest> details = detailRequests == null ? List.of() : detailRequests;
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        List<HrMonthlyLunchDtl> existingDetails = dtlRepository.findByTransactionPoid(transactionPoid);
        Map<Long, HrMonthlyLunchDtl> existingByDetRowId = new HashMap<>();
        long maxDetRowId = 0;
        if (existingDetails != null) {
            for (HrMonthlyLunchDtl e : existingDetails) {
                if (e == null || e.getDetRowId() == null) continue;
                long detRowId = e.getDetRowId();
                existingByDetRowId.put(detRowId, e);
                maxDetRowId = Math.max(maxDetRowId, detRowId);
            }
        }

        Set<Long> deletedDetRowIds = new HashSet<>();
        List<HrMonthlyLunchDtl> toDelete = new ArrayList<>();
        List<HrMonthlyLunchDtl> toUpdate = new ArrayList<>();
        List<HrMonthlyLunchDtl> toCreate = new ArrayList<>();

        for (HrLunchDeductionDtlRequest d : details) {
            if (d == null) continue;

            String action = resolveDetailActionType(d.getActionType(), d.getDetRowId());
            Long detRowIdNorm = normalizeDetRowId(d.getDetRowId());

            switch (action) {
                case "CREATED":
                    maxDetRowId++;
                    HrMonthlyLunchDtl newDtl = mapper.toDtlEntity(transactionPoid, d, maxDetRowId);
                    toCreate.add(newDtl);
                    break;
                case "UPDATED":
                    if (detRowIdNorm == null) throw new ValidationException("detRowId required for UPDATE action");
                    HrMonthlyLunchDtl dtl = existingByDetRowId.get(detRowIdNorm);
                    if (dtl == null) throw new ResourceNotFoundException("Lunch Detail", "detRowId", detRowIdNorm);
                    updateDetailEntity(dtl, d);
                    toUpdate.add(dtl);
                    break;
                case "DELETED":
                    if (detRowIdNorm == null) throw new ValidationException("detRowId required for DELETE action");
                    HrMonthlyLunchDtl dtlToDel = existingByDetRowId.get(detRowIdNorm);
                    if (dtlToDel == null) throw new ResourceNotFoundException("Lunch Detail", "detRowId", detRowIdNorm);
                    toDelete.add(dtlToDel);
                    deletedDetRowIds.add(detRowIdNorm);
                    break;
            }
        }

        if (!toDelete.isEmpty()) {
            dtlRepository.deleteAll(toDelete);
            toDelete.forEach(deleted -> loggingService.createLogSummaryEntry(docId, docKeyPoid,
                    String.format("Row Deleted from Lunch Detail with detRowId: %s", deleted.getDetRowId())));
        }
        if (!toCreate.isEmpty()) {
            dtlRepository.saveAll(toCreate);
            toCreate.forEach(created -> loggingService.createLogSummaryEntry(docId, docKeyPoid,
                    String.format("Row Created on Lunch Detail with detRowId: %s", created.getDetRowId())));
        }
        if (!toUpdate.isEmpty()) {
            dtlRepository.saveAll(toUpdate);
            toUpdate.forEach(updated -> loggingService.createLogSummaryEntry(docId, docKeyPoid,
                    String.format("Row Updated in Lunch Detail with detRowId: %s", updated.getDetRowId())));
        }
    }

    private void updateDetailEntity(HrMonthlyLunchDtl dtl, HrLunchDeductionDtlRequest request) {
        if (request.getLeaveDays() != null) {
            dtl.setOffDays(request.getLeaveDays());
            if (dtl.getMonthDays() != null) {
                dtl.setTotalDays(dtl.getMonthDays() - request.getLeaveDays());
                if (dtl.getCostPerDay() != null) {
                    dtl.setLunchDeductionAmt(dtl.getCostPerDay().multiply(java.math.BigDecimal.valueOf(dtl.getTotalDays())));
                }
            }
        }
        if (request.getDeductionType() != null) dtl.setDeductionType(request.getDeductionType());
        if (request.getAmount() != null) dtl.setLunchDeductionAmt(request.getAmount());
        if (request.getRemarks() != null) dtl.setRemarks(request.getRemarks());
    }

    private String resolveDetailActionType(String actionType, Long detRowId) {
        if (actionType == null || actionType.isBlank()) {
            return normalizeDetRowId(detRowId) == null ? "CREATED" : "UPDATED";
        }
        return actionType.trim().toUpperCase();
    }

    private Long normalizeDetRowId(Long detRowId) {
        if (detRowId == null || detRowId == 0) return null;
        return detRowId;
    }

    private HrMonthlyLunchHdr getHdr(Long transactionPoid) {
        return hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch Deduction", "transactionPoid", transactionPoid));
    }
}
