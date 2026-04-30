package com.asg.hr.leaverejoin.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.response.ApiResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinEmployeeDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinLeaveDetailsResponse;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinRequest;
import com.asg.hr.leaverejoin.dto.EmployeeLeaveRejoinResponse;
import com.asg.hr.leaverejoin.service.EmployeeLeaveRejoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.error;

@RestController
@RequestMapping("/v1/leave-rejoin")
@RequiredArgsConstructor
@Validated
public class EmployeeLeaveRejoinController {

    private final EmployeeLeaveRejoinService service;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    @Operation(
            summary = "List Leave Rejoin records",
            description = "Returns paginated leave rejoin documents with optional filters and transaction-date range.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> list(
            @ParameterObject Pageable pageable,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestBody(required = false) FilterRequestDto filters
    ) {
        Map<String, Object> result = service.list(filters, startDate, endDate, pageable);
        return ApiResponse.success("Leave rejoin list retrieved successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{transactionPoid}")
    @Operation(
            summary = "Get Leave Rejoin by ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getById(
            @PathVariable @NotNull @Positive Long transactionPoid
    ) {
        EmployeeLeaveRejoinResponse response = service.getById(transactionPoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), transactionPoid.toString());
        return ApiResponse.success("Leave rejoin retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    @Operation(
            summary = "Create Leave Rejoin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody EmployeeLeaveRejoinRequest request
    ) {
        EmployeeLeaveRejoinResponse response = service.create(request);
        return ApiResponse.success("Leave rejoin created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{transactionPoid}")
    @Operation(
            summary = "Update Leave Rejoin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> update(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @Valid @RequestBody EmployeeLeaveRejoinRequest request
    ) {
        EmployeeLeaveRejoinResponse response = service.update(transactionPoid, request);
        return ApiResponse.success("Leave rejoin updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{transactionPoid}")
    @Operation(
            summary = "Delete Leave Rejoin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> delete(
            @PathVariable @NotNull @Positive Long transactionPoid,
            @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        service.delete(transactionPoid, deleteReasonDto);
        return ApiResponse.success("Leave rejoin deleted successfully");
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/employee/{employeePoid}")
    @Operation(
            summary = "Get employee details for Leave Rejoin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getEmployeeDetails(
            @PathVariable @NotNull @Positive Long employeePoid
    ) {
        EmployeeLeaveRejoinEmployeeDetailsResponse response = service.getEmployeeDetails(employeePoid);
        return ApiResponse.success("Employee details fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/employee/{employeePoid}/leave-request/{leaveRequestPoid}")
    @Operation(
            summary = "Get leave request details for Leave Rejoin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getLeaveDetails(
            @PathVariable @NotNull @Positive Long employeePoid,
            @PathVariable @NotNull @Positive Long leaveRequestPoid
    ) {
        EmployeeLeaveRejoinLeaveDetailsResponse response = service.getLeaveDetails(employeePoid, leaveRequestPoid);
        return ApiResponse.success("Leave request details fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Leave Rejoin",
            description = "Generate PDF report for a specific leave rejoin transaction",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Leave rejoin not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid
    ) {
        try {
            byte[] pdf = service.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=employee-rejoining-form-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}
