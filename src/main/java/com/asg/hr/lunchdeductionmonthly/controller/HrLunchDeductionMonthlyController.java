package com.asg.hr.lunchdeductionmonthly.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.lunchdeductionmonthly.dto.*;
import com.asg.hr.lunchdeductionmonthly.service.HrLunchDeductionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/lunch-deduction-monthly")
@Tag(name = "Lunch Deduction Monthly", description = "APIs for managing Lunch Deduction Monthly records (800-115)")
public class HrLunchDeductionMonthlyController {

    private final HrLunchDeductionService lunchDeductionService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> list(@RequestBody(required = false) FilterRequestDto filterRequest,
                                  @RequestParam(required = false) LocalDate startDate,
                                  @RequestParam(required = false) LocalDate endDate,
                                  @ParameterObject Pageable pageable) {
        Map<String, Object> result = lunchDeductionService.list(filterRequest, startDate, endDate, pageable);
        return ApiResponse.success("Lunch deduction records retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") @NotNull @Positive Long transactionPoid) {
        HrLunchDeductionResponse response = lunchDeductionService.getById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return ApiResponse.success("Lunch deduction record retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody HrLunchDeductionRequest request) {
        HrLunchDeductionResponse response = lunchDeductionService.create(request);
        return ApiResponse.success("Lunch deduction record created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") @NotNull @Positive Long transactionPoid,
                                    @Valid @RequestBody HrLunchDeductionRequest request) {
        HrLunchDeductionResponse response = lunchDeductionService.update(transactionPoid, request);
        return ApiResponse.success("Lunch deduction record updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{id}/load")
    public ResponseEntity<?> loadAndProcess(@PathVariable("id") @NotNull @Positive Long transactionPoid) {
        HrLunchDeductionLoadDto response = lunchDeductionService.loadAndProcess(transactionPoid);
        return ApiResponse.success("Lunch details loaded and processed successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") @NotNull @Positive Long transactionPoid,
                                    @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        lunchDeductionService.delete(transactionPoid, deleteReasonDto);
        return ApiResponse.success("Lunch deduction record deleted successfully");
    }
}
