package com.asg.hr.lunchdeduction.util;

import com.asg.common.lib.utility.DateUtil;
import com.asg.hr.lunchdeduction.dto.LunchDeductionDetailRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionDetailResponseDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetail;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyDetailId;
import com.asg.hr.lunchdeduction.entity.LunchDeductionMonthlyHeader;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class LunchDeductionMonthlyMapper {

    /**
     * Creates a new header entity from the API request.
     */
    public LunchDeductionMonthlyHeader toEntity(LunchDeductionMonthlyRequestDto requestDto, Long groupPoid, Long companyPoid) {
        LocalDate transactionDate = requestDto.getTransactionDate() != null
                ? requestDto.getTransactionDate()
                : DateUtil.getCurrentDateInUserTimeZone();

        return LunchDeductionMonthlyHeader.builder()
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .transactionDate(transactionDate)
                .payrollMonth(requestDto.getPayrollMonth())
                .lunchDescription(requestDto.getLunchDescription())
                .remarks(requestDto.getRemarks())
                .deleted("N")
                .details(new ArrayList<>())
                .build();
    }

    /**
     * Updates mutable header fields from the request.
     */
    public void updateHeader(LunchDeductionMonthlyHeader header, LunchDeductionMonthlyRequestDto requestDto) {
        if (requestDto.getTransactionDate() != null) {
            header.setTransactionDate(requestDto.getTransactionDate());
        }
        header.setPayrollMonth(requestDto.getPayrollMonth());
        header.setLunchDescription(requestDto.getLunchDescription());
        header.setRemarks(requestDto.getRemarks());
    }

    /**
     * Creates a new detail entity from the API request.
     */
    public LunchDeductionMonthlyDetail toDetailEntity(LunchDeductionDetailRequestDto requestDto, Long detRowId) {
        LunchDeductionMonthlyDetail detail = LunchDeductionMonthlyDetail.builder()
                .id(LunchDeductionMonthlyDetailId.builder().detRowId(detRowId).build())
                .employeePoid(requestDto.getEmployeePoid())
                .deductionType(requestDto.getDeductionType())
                .monthDays(requestDto.getMonthDays())
                .offDays(requestDto.getOffDays())
                .lunchDays(requestDto.getLunchDays())
                .costPerDay(requestDto.getCostPerDay())
                .userId(requestDto.getUserId())
                .userName(requestDto.getUserName())
                .remarks(requestDto.getRemarks())
                .build();
        recalculateDetail(detail);
        return detail;
    }

    /**
     * Updates mutable detail fields and recomputes derived values.
     */
    public void updateDetail(LunchDeductionMonthlyDetail detail, LunchDeductionDetailRequestDto requestDto) {
        if (requestDto.getEmployeePoid() != null) {
            detail.setEmployeePoid(requestDto.getEmployeePoid());
        }
        if (requestDto.getDeductionType() != null) {
            detail.setDeductionType(requestDto.getDeductionType());
        }
        if (requestDto.getMonthDays() != null) {
            detail.setMonthDays(requestDto.getMonthDays());
        }
        if (requestDto.getOffDays() != null) {
            detail.setOffDays(requestDto.getOffDays());
        }
        if (requestDto.getLunchDays() != null) {
            detail.setLunchDays(requestDto.getLunchDays());
        }
        if (requestDto.getCostPerDay() != null) {
            detail.setCostPerDay(requestDto.getCostPerDay());
        }
        if (requestDto.getUserId() != null) {
            detail.setUserId(requestDto.getUserId());
        }
        if (requestDto.getUserName() != null) {
            detail.setUserName(requestDto.getUserName());
        }
        detail.setRemarks(requestDto.getRemarks());
        recalculateDetail(detail);
    }

    /**
     * Recalculates derived attendance values for a detail row.
     */
    public void recalculateDetail(LunchDeductionMonthlyDetail detail) {
        if (detail.getMonthDays() == null) {
            detail.setTotalDays(null);
            detail.setLunchDeductionAmt(null);
            return;
        }

        long totalDays = detail.getMonthDays() - defaultLong(detail.getOffDays());
        detail.setTotalDays(totalDays);

        if (detail.getCostPerDay() == null) {
            detail.setLunchDeductionAmt(null);
            return;
        }

        detail.setLunchDeductionAmt(detail.getCostPerDay().multiply(BigDecimal.valueOf(totalDays)));
    }

    /**
     * Builds the API response payload with enriched detail totals.
     */
    public LunchDeductionMonthlyResponseDto toResponse(LunchDeductionMonthlyHeader header) {
        List<LunchDeductionDetailResponseDto> details = header.getDetails() == null
                ? List.of()
                : header.getDetails().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(detail -> detail.getId().getDetRowId()))
                .map(this::toDetailResponse)
                .toList();

        long totalLunchDays = details.stream()
                .map(LunchDeductionDetailResponseDto::getLunchDays)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        BigDecimal totalDeductionAmount = details.stream()
                .map(LunchDeductionDetailResponseDto::getLunchDeductionAmt)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return LunchDeductionMonthlyResponseDto.builder()
                .transactionPoid(header.getTransactionPoid())
                .groupPoid(header.getGroupPoid())
                .companyPoid(header.getCompanyPoid())
                .transactionDate(header.getTransactionDate())
                .docRef(header.getDocRef())
                .payrollMonth(header.getPayrollMonth())
                .lunchDescription(header.getLunchDescription())
                .remarks(header.getRemarks())
                .deleted(header.getDeleted())
                .createdBy(header.getCreatedBy())
                .createdDate(header.getCreatedDate())
                .lastModifiedBy(header.getLastModifiedBy())
                .lastModifiedDate(header.getLastModifiedDate())
                .details(details)
                .detailCount((long) details.size())
                .totalLunchDays(totalLunchDays)
                .totalDeductionAmount(totalDeductionAmount)
                .build();
    }

    /**
     * Creates a shallow snapshot for change logging.
     */
    public LunchDeductionMonthlyHeader copyForLogging(LunchDeductionMonthlyHeader header) {
        return LunchDeductionMonthlyHeader.builder()
                .transactionPoid(header.getTransactionPoid())
                .groupPoid(header.getGroupPoid())
                .companyPoid(header.getCompanyPoid())
                .transactionDate(header.getTransactionDate())
                .docRef(header.getDocRef())
                .payrollMonth(header.getPayrollMonth())
                .lunchDescription(header.getLunchDescription())
                .remarks(header.getRemarks())
                .deleted(header.getDeleted())
                .details(new ArrayList<>())
                .build();
    }

    private LunchDeductionDetailResponseDto toDetailResponse(LunchDeductionMonthlyDetail detail) {
        return LunchDeductionDetailResponseDto.builder()
                .detRowId(detail.getId().getDetRowId())
                .employeePoid(detail.getEmployeePoid())
                .deductionType(detail.getDeductionType())
                .monthDays(detail.getMonthDays())
                .offDays(detail.getOffDays())
                .totalDays(detail.getTotalDays())
                .costPerDay(detail.getCostPerDay())
                .lunchDeductionAmt(detail.getLunchDeductionAmt())
                .remarks(detail.getRemarks())
                .lunchDays(detail.getLunchDays())
                .userId(detail.getUserId())
                .userName(detail.getUserName())
                .createdBy(detail.getCreatedBy())
                .createdDate(detail.getCreatedDate())
                .lastModifiedBy(detail.getLastModifiedBy())
                .lastModifiedDate(detail.getLastModifiedDate())
                .build();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}