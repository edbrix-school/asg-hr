package com.asg.hr.lunchdeduction.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface LunchDeductionMonthlyService {

    /**
     * Lists lunch deduction monthly documents using shared document-search behavior.
     */
    Map<String, Object> listLunchDeductions(String documentId, FilterRequestDto filterRequestDto, Pageable pageable);

    /**
     * Returns one lunch deduction document with its detail rows.
     */
    LunchDeductionMonthlyResponseDto getById(Long transactionPoid);

    /**
     * Returns enriched lunch deduction details for view and LOV style access.
     */
    LunchDeductionMonthlyResponseDto getDetails(Long transactionPoid);

    /**
     * Creates a lunch deduction monthly document.
     */
    LunchDeductionMonthlyResponseDto create(LunchDeductionMonthlyRequestDto requestDto);

    /**
     * Updates a lunch deduction monthly document and its child rows.
     */
    LunchDeductionMonthlyResponseDto update(Long transactionPoid, LunchDeductionMonthlyRequestDto requestDto);

    /**
     * Soft deletes a lunch deduction monthly document.
     */
    void delete(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Loads and recalculates detail rows by executing the legacy import procedure.
     */
    LunchDeductionMonthlyResponseDto importLunchDetails(Long transactionPoid);
}