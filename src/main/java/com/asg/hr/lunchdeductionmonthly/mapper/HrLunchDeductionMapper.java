package com.asg.hr.lunchdeductionmonthly.mapper;

import com.asg.hr.lunchdeductionmonthly.dto.*;
import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchDtl;
import com.asg.hr.lunchdeductionmonthly.entity.HrMonthlyLunchHdr;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class HrLunchDeductionMapper {

    public HrMonthlyLunchHdr toEntity(HrLunchDeductionRequest request) {
        if (request == null) return null;
        return HrMonthlyLunchHdr.builder()
                .payrollMonth(request.getPayrollMonth())
                .description(request.getDescription())
                .remarks(request.getRemarks())
                .deleted("N")
                .build();
    }

    public void updateEntity(HrMonthlyLunchHdr hdr, HrLunchDeductionRequest request) {
        if (hdr == null || request == null) return;
        hdr.setDescription(request.getDescription());
        hdr.setRemarks(request.getRemarks());
    }

    public void updateEntity(HrMonthlyLunchHdr hdr, HrLunchDeductionUpdateRequest request) {
        if (hdr == null || request == null) return;
        hdr.setDescription(request.getDescription());
        hdr.setRemarks(request.getRemarks());
    }

    public HrLunchDeductionResponse toResponse(HrMonthlyLunchHdr hdr) {
        if (hdr == null) return null;
        return HrLunchDeductionResponse.builder()
                .transactionPoid(hdr.getTransactionPoid())
                .docRef(hdr.getDocRef())
                .payrollMonth(hdr.getPayrollMonth())
                .description(hdr.getDescription())
                .remarks(hdr.getRemarks())
                .build();
    }

    public HrLunchDeductionDtlResponse toDtlResponse(HrMonthlyLunchDtl dtl) {
        if (dtl == null) return null;
        return HrLunchDeductionDtlResponse.builder()
                .detRowId(dtl.getDetRowId())
                .transactionPoid(dtl.getTransactionPoid())
                .employeePoid(dtl.getEmployeePoid())
                .employeeName(dtl.getUserName())
                .deductionType(dtl.getDeductionType())
                .lunchDays(dtl.getLunchDays())
                .monthDays(dtl.getMonthDays())
                .offDays(dtl.getOffDays())
                .totalDays(dtl.getTotalDays())
                .costPerDay(dtl.getCostPerDay())
                .amount(dtl.getLunchDeductionAmt())
                .remarks(dtl.getRemarks())
                .build();
    }

    public List<HrLunchDeductionDtlResponse> toDtlResponseList(List<HrMonthlyLunchDtl> dtlList) {
        if (dtlList == null) return Collections.emptyList();
        return dtlList.stream().map(this::toDtlResponse).toList();
    }

    public List<HrMonthlyLunchDtl> toDtlEntityList(Long transactionPoid, List<HrLunchDeductionDtlRequest> dtlRequests) {
        if (dtlRequests == null) return Collections.emptyList();
        List<HrMonthlyLunchDtl> result = new java.util.ArrayList<>();
        for (int i = 0; i < dtlRequests.size(); i++) {
            HrLunchDeductionDtlRequest req = dtlRequests.get(i);
            if (req != null) {
                result.add(toDtlEntity(transactionPoid, req, (long) (i + 1)));
            }
        }
        return result;
    }

    public HrMonthlyLunchDtl toDtlEntity(Long transactionPoid, HrLunchDeductionDtlRequest request, Long detRowId) {
        if (request == null) return null;
        return HrMonthlyLunchDtl.builder()
                .detRowId(detRowId)
                .transactionPoid(transactionPoid)
                .deductionType(request.getDeductionType())
                .offDays(request.getLeaveDays())
                .lunchDeductionAmt(request.getAmount())
                .remarks(request.getRemarks())
                .build();
    }
}
