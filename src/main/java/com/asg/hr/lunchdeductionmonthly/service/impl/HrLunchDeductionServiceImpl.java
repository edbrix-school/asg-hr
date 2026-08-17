package com.asg.hr.lunchdeductionmonthly.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
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
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final LovDataService lovService;

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
                String logDetail = String.format("KeyId = detRowId:%s", entity.getDetRowId());
                loggingService.createLog(null, entity, HrMonthlyLunchDtl.class, docId, docKeyPoid, logDetail);
            }
        }

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                saved.getTransactionPoid().toString(),
                LogDetailsEnum.CREATED.getDescription() + " " + saved.getDocRef()
        );
        
        HrLunchDeductionResponse response = mapper.toResponse(saved);
        response.setDetails(mapper.toDtlResponseList(detailEntities));
        enrichWithLovData(response);
        return response;
    }

    @Override
    @Transactional
    public HrLunchDeductionResponse update(Long transactionPoid, HrLunchDeductionRequest request) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);
        HrMonthlyLunchHdr oldCopy = snapshot(hdr);

        mapper.updateEntity(hdr, request);
        HrMonthlyLunchHdr saved = hdrRepository.saveAndFlush(hdr);

        updateLunchDeductionDetails(transactionPoid, request.getDetails());

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        loggingService.logChanges(oldCopy, saved, HrMonthlyLunchHdr.class, docId, docKeyPoid, LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        List<HrMonthlyLunchDtl> updatedDetails = dtlRepository.findByTransactionPoid(transactionPoid);
        HrLunchDeductionResponse response = mapper.toResponse(saved);
        response.setDetails(mapper.toDtlResponseList(updatedDetails));
        enrichWithLovData(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public HrLunchDeductionResponse getById(Long transactionPoid) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);
        List<HrMonthlyLunchDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        HrLunchDeductionResponse response = mapper.toResponse(hdr);
        response.setDetails(mapper.toDtlResponseList(details));
        enrichWithLovData(response);
        return response;
    }

    @Override
    @Transactional
    public HrLunchDeductionLoadDto loadAndProcess(Long transactionPoid) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);

        procRepository.loadLunchDetails(transactionPoid, UserContext.getUserPoid(), hdr.getPayrollMonth());

        List<HrMonthlyLunchDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        List<HrLunchDeductionDtlResponse> dtlResponses = mapper.toDtlResponseList(details);
        enrichDtlWithLovData(dtlResponses);
        return HrLunchDeductionLoadDto.builder()
                .lunchDetails(dtlResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(FilterRequestDto filterRequest, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filters = documentSearchService.resolveDateFilters(filterRequest, "PAYROLL_MONTH", startDate, endDate);

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
        Map<Long, HrMonthlyLunchDtl> oldByDetRowId = new HashMap<>();

        for (HrLunchDeductionDtlRequest d : details) {
            if (d == null) continue;

            String action = resolveDetailActionType(d.getActionType(), d.getDetRowId());
            Long detRowIdNorm = normalizeDetRowId(d.getDetRowId());

            switch (action) {
                case "isCreated":
                    maxDetRowId++;
                    HrMonthlyLunchDtl newDtl = mapper.toDtlEntity(transactionPoid, d, maxDetRowId);
                    toCreate.add(newDtl);
                    break;
                case "isUpdated":
                    if (detRowIdNorm == null) throw new ValidationException("detRowId required for UPDATE action");
                    HrMonthlyLunchDtl dtl = existingByDetRowId.get(detRowIdNorm);
                    if (dtl == null) throw new ResourceNotFoundException("Lunch Detail", "detRowId", detRowIdNorm);
                    oldByDetRowId.put(detRowIdNorm, snapshotDetail(dtl));
                    updateDetailEntity(dtl, d);
                    toUpdate.add(dtl);
                    break;
                case "isDeleted":
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
            toDelete.forEach(deleted -> loggingService.logDelete(deleted, docId, docKeyPoid));
        }
        if (!toCreate.isEmpty()) {
            dtlRepository.saveAll(toCreate);
            toCreate.forEach(created -> loggingService.createLog(null, created, HrMonthlyLunchDtl.class, docId, docKeyPoid,
                    String.format("KeyId = detRowId:%s", created.getDetRowId())));
        }
        if (!toUpdate.isEmpty()) {
            dtlRepository.saveAll(toUpdate);
            toUpdate.forEach(updated -> loggingService.createLog(oldByDetRowId.get(updated.getDetRowId()), updated, HrMonthlyLunchDtl.class, docId, docKeyPoid,
                    String.format("KeyId = detRowId:%s", updated.getDetRowId())));
        }
    }

    private void updateDetailEntity(HrMonthlyLunchDtl dtl, HrLunchDeductionDtlRequest request) {
        if (request.getLeaveDays() != null) {
            if (dtl.getMonthDays() == null)
                throw new ValidationException("Cannot update leave days: month days not set for detRowId: " + dtl.getDetRowId());
            if (request.getLeaveDays() < 0 || request.getLeaveDays() > dtl.getMonthDays())
                throw new ValidationException("Leave days must be between 0 and month days (" + dtl.getMonthDays() + ")");
            dtl.setOffDays(request.getLeaveDays());
            long totalDays = dtl.getMonthDays() - request.getLeaveDays();
            dtl.setTotalDays(totalDays);
            if (dtl.getCostPerDay() != null) {
                dtl.setLunchDeductionAmt(dtl.getCostPerDay().multiply(java.math.BigDecimal.valueOf(totalDays)));
            }
        }
        // deductionType is non-editable after creation
        if (request.getLunchDays() != null) dtl.setLunchDays(request.getLunchDays());
        if (request.getAmount() != null) dtl.setLunchDeductionAmt(request.getAmount());
        if (request.getRemarks() != null) dtl.setRemarks(request.getRemarks());
    }

    private void enrichWithLovData(HrLunchDeductionResponse response) {
        if (response == null || response.getDetails() == null || response.getDetails().isEmpty()) return;
        enrichDtlWithLovData(response.getDetails());
    }

    private void enrichDtlWithLovData(List<HrLunchDeductionDtlResponse> details) {
        if (details == null || details.isEmpty()) return;
        for (HrLunchDeductionDtlResponse detail : details) {
            if (detail == null) continue;
            if (detail.getEmployeePoid() != null) {
                detail.setEmpDet(lovService.getDetailsByPoidAndLovName(detail.getEmployeePoid(), "EMPLOYEE_NAME"));
            }
            if (detail.getDeductionType() != null) {
                detail.setDeductionTypeDet(lovService.getDetailsByCodeAndLovName(detail.getDeductionType(), "LUNCH_DEDUCTION_TYPE"));
            }
        }
    }

    private String resolveDetailActionType(String actionType, Long detRowId) {
        if (actionType == null || actionType.isBlank()) {
            return normalizeDetRowId(detRowId) == null ? "isCreated" : "isUpdated";
        }
        return actionType.trim();
    }

    private Long normalizeDetRowId(Long detRowId) {
        if (detRowId == null || detRowId == 0) return null;
        return detRowId;
    }

    private HrMonthlyLunchHdr snapshot(HrMonthlyLunchHdr hdr) {
        HrMonthlyLunchHdr copy = new HrMonthlyLunchHdr();
        BeanUtils.copyProperties(hdr, copy);
        return copy;
    }

    private HrMonthlyLunchDtl snapshotDetail(HrMonthlyLunchDtl dtl) {
        HrMonthlyLunchDtl copy = new HrMonthlyLunchDtl();
        BeanUtils.copyProperties(dtl, copy);
        return copy;
    }

    private HrMonthlyLunchHdr getHdr(Long transactionPoid) {
        return hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch Deduction", "transactionPoid", transactionPoid));
    }
}
