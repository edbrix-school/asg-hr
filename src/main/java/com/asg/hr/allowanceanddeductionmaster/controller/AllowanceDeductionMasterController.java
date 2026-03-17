package com.asg.hr.allowanceanddeductionmaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.allowanceanddeductionmaster.dto.*;
import com.asg.hr.allowanceanddeductionmaster.service.AllowanceDeductionMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/allowance-deduction-master")
@RequiredArgsConstructor
@Slf4j
public class AllowanceDeductionMasterController {

    private final AllowanceDeductionMasterService service;
    private final LoggingService loggingService;

    @Operation(
            summary = "Create Allowance/Deduction",
            description = "Creates a new allowance or deduction component",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AllowanceDeductionRequestDTO request) {
        log.info("Creating allowance/deduction with code: {}", request.getCode());
        try {
            AllowanceDeductionResponseDTO response = service.create(request);
            return success("Allowance/Deduction created successfully", response);
        } catch (Exception ex) {
            log.error("Failed to create allowance/deduction", ex);
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Update Allowance/Deduction",
            description = "Updates an existing allowance/deduction component",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{allowaceDeductionPoid}")
    public ResponseEntity<?> update(
            @Parameter(description = "Allowance/Deduction ID", required = true)
            @PathVariable Long allowaceDeductionPoid,
            @Valid @RequestBody AllowanceDeductionRequestDTO request) {
        log.info("Updating allowance/deduction with id: {}", allowaceDeductionPoid);
        try {
            AllowanceDeductionResponseDTO response = service.update(allowaceDeductionPoid, request);
            return success("Allowance/Deduction updated successfully", response);
        } catch (Exception ex) {
            log.error("Failed to update allowance/deduction with id: {}", allowaceDeductionPoid, ex);
            return internalServerError(ex.getMessage());
        }
    }

    @Operation(
            summary = "Get Allowance/Deduction by ID",
            description = "Retrieves allowance/deduction details by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{allowaceDeductionPoid}")
    public ResponseEntity<?> getById(
            @Parameter(description = "Allowance/Deduction ID", required = true)
            @PathVariable Long allowaceDeductionPoid) {
        log.info("Getting allowance/deduction with id: {}", allowaceDeductionPoid);
        try {
            AllowanceDeductionResponseDTO response = service.getById(allowaceDeductionPoid);
            loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), allowaceDeductionPoid.toString());
            return success("Allowance/Deduction fetched successfully", response);
        } catch (Exception ex) {
            log.error("Failed to get allowance/deduction with id: {}", allowaceDeductionPoid, ex);
            return notFound(ex.getMessage());
        }
    }

    @Operation(
            summary = "Search Allowance/Deductions",
            description = """
                    Fetch Allowance/Deductions using pagination + dynamic filters.
                    
                    ### Allowed searchField values:
                    ALLOWACE_DEDUCTION_POID, CODE, DESCRIPTION, TYPE, ACTIVE, 
                    CREATED_DATE, VARIABLE_FIXED
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Search Filters for Allowance/Deductions",
            content = @Content(
                    schema = @Schema(implementation = FilterRequestDto.class),
                    examples = @ExampleObject(
                            name = "Allowance/Deduction Filters",
                            value = """
                                    {
                                      "operator": "AND",
                                      "isDeleted": "N",
                                      "filters": [
                                        { "searchField": "ACTIVE", "searchValue": "Y" },
                                        { "searchField": "TYPE", "searchValue": "ALLOWANCE" }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/search")
    public ResponseEntity<?> search(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto filterRequest) {
        log.info("Searching allowance/deductions with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            Map<String, Object> result = service.search(filterRequest,startDate,endDate, pageable);
            return success("Allowance/Deductions retrieved successfully", result);
        } catch (Exception ex) {
            log.error("Failed to search allowance/deductions", ex);
            return internalServerError("Failed to search: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Delete Allowance/Deduction",
            description = "Soft deletes an allowance/deduction component",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            },
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{allowaceDeductionPoid}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Allowance/Deduction ID", required = true)
            @PathVariable Long allowaceDeductionPoid,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Deleting allowance/deduction with id: {}", allowaceDeductionPoid);
        try {
            service.delete(allowaceDeductionPoid, deleteReasonDto);
            return success("Allowance/Deduction deleted successfully");
        } catch (Exception ex) {
            log.error("Failed to delete allowance/deduction with id: {}", allowaceDeductionPoid, ex);
            return notFound(ex.getMessage());
        }
    }
}
