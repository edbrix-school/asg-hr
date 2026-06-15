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
import com.asg.hr.lunchdeductionmonthly.entity.key.HrMonthlyLunchDtlKey;
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

import java.util.List;
import java.util.Map;

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

        loggingService.createLogSummaryEntry(
                UserContext.getDocumentId(),
                saved.getTransactionPoid().toString(),
                LogDetailsEnum.CREATED.getDescription() + " " + saved.getDocRef()
        );
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HrLunchDeductionResponse update(Long transactionPoid, HrLunchDeductionUpdateRequest request) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);

        mapper.updateEntity(hdr, request);
        HrMonthlyLunchHdr saved = hdrRepository.saveAndFlush(hdr);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), transactionPoid.toString());
        return mapper.toResponse(saved);
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
    @Transactional
    public void updateDetail(Long transactionPoid, HrLunchDeductionDtlRequest dtlRequest) {
        HrMonthlyLunchHdr hdr = getHdr(transactionPoid);
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        HrMonthlyLunchDtlKey key = new HrMonthlyLunchDtlKey(dtlRequest.getDetRowId(), transactionPoid);
        HrMonthlyLunchDtl dtl = dtlRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch Detail", "detRowId", dtlRequest.getDetRowId()));

        String actionType = dtlRequest.getActionType() != null ? dtlRequest.getActionType().trim() : "UPDATED";

        if (dtlRequest.getLeaveDays() != null) {
            dtl.setOffDays(dtlRequest.getLeaveDays());
            if (dtl.getMonthDays() != null) {
                dtl.setTotalDays(dtl.getMonthDays() - dtlRequest.getLeaveDays());
                if (dtl.getCostPerDay() != null) {
                    dtl.setLunchDeductionAmt(dtl.getCostPerDay().multiply(java.math.BigDecimal.valueOf(dtl.getTotalDays())));
                }
            }
        }
        if (dtlRequest.getDeductionType() != null) dtl.setDeductionType(dtlRequest.getDeductionType());
        if (dtlRequest.getAmount() != null) dtl.setLunchDeductionAmt(dtlRequest.getAmount());
        if (dtlRequest.getRemarks() != null) dtl.setRemarks(dtlRequest.getRemarks());

        dtlRepository.save(dtl);

        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s Action: %s", 
                transactionPoid, dtlRequest.getDetRowId(), actionType);
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
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

    private HrMonthlyLunchHdr getHdr(Long transactionPoid) {
        return hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch Deduction", "transactionPoid", transactionPoid));
    }
}
