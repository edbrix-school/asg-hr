package com.asg.hr.resignation.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.resignation.dto.HrResignationEmployeeDetailsResponse;
import com.asg.hr.resignation.dto.HrResignationRequest;
import com.asg.hr.resignation.dto.HrResignationResponse;
import com.asg.hr.resignation.service.HrResignationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/v1/resignation")
@RequiredArgsConstructor
@Validated
public class HrResignationController {

    private final HrResignationService resignationService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(
            summary = "List Resignations",
            description = "Returns paginated resignation documents with filters/search.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> listResignations(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto filters
    ) {
        Map<String, Object> result = resignationService.listResignations(filters, startDate, endDate, pageable);
        return ApiResponse.success("Resignation list retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get Resignation by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getById(
            @PathVariable @NotNull @Positive Long transactionPoid
    ) {
        HrResignationResponse response = resignationService.getById(transactionPoid);

        String docId = UserContext.getDocumentId();
        if (docId == null || docId.isBlank()) {
            throw new ValidationException("Document id is mandatory");
        }
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, docId, transactionPoid.toString());

        return ApiResponse.success("Resignation retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Resignation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody HrResignationRequest request
    ) {
        HrResignationResponse response = resignationService.create(request);
        return ApiResponse.success("Resignation created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update Resignation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody HrResignationRequest request
    ) {
        HrResignationResponse response = resignationService.update(transactionPoid, request);
        return ApiResponse.success("Resignation updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete Resignation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> delete(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        resignationService.delete(transactionPoid, deleteReasonDto);
        return ApiResponse.success("Resignation deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/employee/{employeePoid}")
    @Operation(
            summary = "Get Employee Details for Resignation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getEmployeeDetails(
            @PathVariable @NotNull @Positive Long employeePoid
    ) {
        HrResignationEmployeeDetailsResponse response = resignationService.getEmployeeDetails(employeePoid);
        return ApiResponse.success("Employee details fetched successfully", response);
    }

}

