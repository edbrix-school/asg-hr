package com.asg.hr.employeemaster.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.hr.employeemaster.dto.*;
import com.asg.hr.employeemaster.service.EmployeeMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.error;
import static com.asg.common.lib.dto.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/employee-master")
@Tag(name = "Employee Master", description = "APIs for managing Employee Master records")
public class EmployeeMasterController {

    private final EmployeeMasterService employeeMasterService;
    private final LoggingService loggingService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/dashboard-details")
    public ResponseEntity<?> getEmployeeMasterDashboardDetails() {
        return success("Employee master dashboard details fetched successfully", employeeMasterService.getEmployeeCounts());
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/dashboard-details/list")
    public ResponseEntity<?> listEmployeeMasterDashboardDetails(@ParameterObject Pageable pageable, @RequestBody(required = false) EmployeeDashboardListRequestDto request) {
        Map<String, Object> result = employeeMasterService.listEmployeeDashboardDetails(request, pageable);
        return success("Employee master dashboard list fetched successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/list")
    public ResponseEntity<?> listEmployees(@ParameterObject Pageable pageable, @RequestBody(required = false) FilterRequestDto filters) {
        Map<String, Object> result = employeeMasterService.listEmployees(UserContext.getDocumentId(), filters, pageable);
        return success("Employee list fetched successfully", result);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/{employeePoid}")
    public ResponseEntity<?> getById(@PathVariable @NotNull Long employeePoid) {
        EmployeeMasterResponseDto response = employeeMasterService.getEmployeeById(employeePoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), employeePoid.toString());
        return success("Employee retrieved successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping
    public ResponseEntity<?> createEmployee(@Valid @RequestBody EmployeeMasterRequestDto requestDto) {
        EmployeeMasterResponseDto response = employeeMasterService.createEmployee(requestDto);
        return success("Employee created successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/{employeePoid}")
    public ResponseEntity<?> updateEmployee(@PathVariable @NotNull Long employeePoid, @Valid @RequestBody EmployeeMasterRequestDto requestDto
    ) {
        EmployeeMasterResponseDto response = employeeMasterService.updateEmployee(employeePoid, requestDto);
        return success("Employee updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.DELETE)
    @DeleteMapping("/{employeePoid}")
    public ResponseEntity<?> deleteEmployee(@PathVariable @NotNull Long employeePoid, @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto
    ) {
        employeeMasterService.deleteEmployee(employeePoid, deleteReasonDto);
        return success("Employee deleted successfully");
    }

    /**
     * Separate endpoint for HR_EMPLOYEE_MASTER.PHOTO.
     * Frontend calls this after create, and provides EMPLOYEE_POID + PHOTO.
     */
    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PutMapping("/photo")
    public ResponseEntity<?> updateEmployeePhoto(@RequestParam @NotNull Long employeePoid, @Valid @RequestBody EmployeePhotoUpdateRequestDto requestDto
    ) {
        EmployeePhotoUpdateResponseDto response = employeeMasterService.updateEmployeePhoto(employeePoid, requestDto);
        return success("Employee photo updated successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.PRINT)
    @Operation(
            summary = "Generate PDF for Employee Master",
            description = "Generate PDF report for a specific Employee Profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Employee not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to generate PDF")
            }
    )
    @GetMapping("/print/{transactionPoid}")
    public ResponseEntity<?> print(
            @Parameter(description = "Transaction POID", example = "21")
            @PathVariable Long transactionPoid) {
        try {
            byte[] pdf = employeeMasterService.print(transactionPoid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=employee-profile-" + transactionPoid + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return error("Failed to generate PDF: " + e.getMessage(), 500);
        }
    }
}

