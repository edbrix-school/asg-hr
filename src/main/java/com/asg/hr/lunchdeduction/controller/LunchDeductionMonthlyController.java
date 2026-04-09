package com.asg.hr.lunchdeduction.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyRequestDto;
import com.asg.hr.lunchdeduction.dto.LunchDeductionMonthlyResponseDto;
import com.asg.hr.lunchdeduction.service.LunchDeductionMonthlyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/lunch-deduction")
@RequiredArgsConstructor
@Validated
@Slf4j
public class LunchDeductionMonthlyController {

    private final LunchDeductionMonthlyService lunchDeductionMonthlyService;
    private final LoggingService loggingService;

    /**
     * Lists lunch deduction monthly documents with filtering and pagination.
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(summary = "List lunch deduction monthly records", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestBody(required = false) FilterRequestDto filterRequestDto
    ) {
        log.info("List Lunch Deduction request | page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Map<String, Object> result = lunchDeductionMonthlyService.listLunchDeductions(UserContext.getDocumentId(), filterRequestDto, pageable);
        return ApiResponse.success("Lunch deduction list fetched successfully", result);
    }

    /**
     * Returns a lunch deduction monthly document by its transaction id.
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(summary = "Get lunch deduction monthly document", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getById(@PathVariable @NotNull @Positive Long transactionPoid) {
        log.info("Get Lunch Deduction request | transactionPoid={}", transactionPoid);
        LunchDeductionMonthlyResponseDto responseDto = lunchDeductionMonthlyService.getById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return ApiResponse.success("Lunch deduction fetched successfully", responseDto);
    }

    /**
     * Returns enriched document details for LOV style access and view screens.
     */
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}/details")
    @Operation(summary = "Get lunch deduction monthly details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getDetails(@PathVariable @NotNull @Positive Long transactionPoid) {
        log.info("Get Lunch Deduction details request | transactionPoid={}", transactionPoid);
        LunchDeductionMonthlyResponseDto responseDto = lunchDeductionMonthlyService.getDetails(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return ApiResponse.success("Lunch deduction details fetched successfully", responseDto);
    }

    /**
     * Creates a lunch deduction monthly document.
     */
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(summary = "Create lunch deduction monthly document", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> create(@Valid @RequestBody LunchDeductionMonthlyRequestDto requestDto) {
        log.info("Create Lunch Deduction request | payrollMonth={}, userId={}", requestDto.getPayrollMonth(), UserContext.getUserId());
        LunchDeductionMonthlyResponseDto responseDto = lunchDeductionMonthlyService.create(requestDto);
        return ApiResponse.success("Lunch deduction created successfully", responseDto);
    }

    /**
     * Updates a lunch deduction monthly document.
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(summary = "Update lunch deduction monthly document", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody LunchDeductionMonthlyRequestDto requestDto
    ) {
        log.info("Update Lunch Deduction request | transactionPoid={}, payrollMonth={}, userId={}",
                transactionPoid,
                requestDto.getPayrollMonth(),
                UserContext.getUserId());
        LunchDeductionMonthlyResponseDto responseDto = lunchDeductionMonthlyService.update(transactionPoid, requestDto);
        return ApiResponse.success("Lunch deduction updated successfully", responseDto);
    }

    /**
     * Soft deletes a lunch deduction monthly document.
     */
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(summary = "Delete lunch deduction monthly document", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        log.info("Delete Lunch Deduction request | transactionPoid={}, userId={}", transactionPoid, UserContext.getUserId());
        lunchDeductionMonthlyService.delete(transactionPoid, deleteReasonDto);
        return ApiResponse.success("Lunch deduction deleted successfully");
    }

    /**
     * Executes the legacy import procedure to load attendance-based detail rows.
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/{transactionPoid}/import")
    @Operation(summary = "Import lunch deduction attendance", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> importLunchDetails(@PathVariable @NotNull @Positive Long transactionPoid) {
        log.info("Import Lunch Deduction request | transactionPoid={}, userPoid={}", transactionPoid, UserContext.getUserPoid());
        LunchDeductionMonthlyResponseDto responseDto = lunchDeductionMonthlyService.importLunchDetails(transactionPoid);
        return ApiResponse.success("Lunch deduction details imported successfully", responseDto);
    }
}